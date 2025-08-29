package com.wzz.hslspringboot.webSocketServer;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.json.JSONUtil;
import com.wzz.hslspringboot.DTO.Result;
import com.wzz.hslspringboot.DTO.WebSocketDTO;
import com.wzz.hslspringboot.modules.ServiceExecutor;
import com.wzz.hslspringboot.pojo.UserSmsWebSocket;
import com.wzz.hslspringboot.service.UserSmsWebSocketService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WsHandler extends TextWebSocketHandler {

    private static final Logger log = LogManager.getLogger(WsHandler.class);
    @Autowired
    private UserSmsWebSocketService userSmsWebSocketService;

    // 依然使用 userPhone 作为 Key 来管理 WebSocket 会话
    private static final Map<String, WebSocketSession> userSessionMap = new ConcurrentHashMap<>();
    // 依然将会话属性的 Key 定义为常量，便于在连接关闭时获取
    private static final String USER_PHONE_KEY = "user";

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("新的WebSocket连接已建立, SessionId: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            log.info("收到来自, SessionId: {} 的消息: {}", session.getId(),payload);
            WebSocketDTO messageDTO = JSONUtil.toBean(payload, WebSocketDTO.class);

            // 1. 核心校验：任何消息都必须包含 user, server, method
            if (messageDTO == null || messageDTO.getUser() == null || messageDTO.getUser().isEmpty()
                    || messageDTO.getServer() == null || messageDTO.getServer().isEmpty()
                    || messageDTO.getMethod() == null || messageDTO.getMethod().isEmpty()) {
                log.error("无效指令: 消息必须包含 'user', 'server', 'method' 字段。");
                sendMessage(session, Result.error("无效指令: 消息必须包含 'user', 'server', 'method' 字段。"));
                return;
            }

            String userPhone = messageDTO.getUser();

            // 2. 即时注册逻辑
            // 检查当前会话是否已经绑定了用户手机号。如果没有，就进行绑定。
            if (session.getAttributes().get(USER_PHONE_KEY) == null) {
                // 如果该用户已在别处登录（存在旧会话），则断开旧会话
                WebSocketSession oldSession = userSessionMap.get(userPhone);
                if (oldSession != null && oldSession.isOpen()) {
                    log.info("用户 {} 从新位置连接，关闭旧的会话: {}", userPhone, oldSession.getId());
                    oldSession.close();
                }

                // 绑定当前新会话
                session.getAttributes().put(USER_PHONE_KEY, userPhone);
                userSessionMap.put(userPhone, session);

                // --- 数据库操作 ---
                // 更新用户状态为在线
                 UserSmsWebSocket userSms = new UserSmsWebSocket();
                 userSms.setUserPhone(userPhone);
                 userSms.setStatus(true);
                 userSmsWebSocketService.saveUserWsaocket(userSms); // 示例
                log.info("用户 {} 首次通信，已自动注册并绑定会话: {}", userPhone, session.getId());
            }

            // 3. 执行指令
            // 参数处理
            Object[] argsArray;
            if (messageDTO.getArgs() == null) {
                argsArray = new Object[0];
            } else if (messageDTO.getArgs() instanceof List) {
                argsArray = ((List<?>) messageDTO.getArgs()).toArray();
            } else {
                // 如果参数不是列表，而是单个值（如字符串、数字），包装成单元素数组
                argsArray = new Object[]{messageDTO.getArgs()};
            }

            // 动态执行方法
            Object result = ServiceExecutor.dispatch(messageDTO.getServer(), messageDTO.getMethod(), argsArray);

            // 返回执行结果
            sendMessage(session, Result.success(result));

        } catch (Exception e) {
            log.error("处理消息时发生错误: {}", e.getMessage());
            sendMessage(session, Result.error("服务器内部错误: " + ExceptionUtil.getRootCauseMessage(e)));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String userPhone = (String) session.getAttributes().get(USER_PHONE_KEY);

        if (userPhone != null) {
            try {
                // 只移除与当前关闭的 session 完全匹配的条目
                userSessionMap.remove(userPhone, session);

                UserSmsWebSocket userSms = new UserSmsWebSocket();
                userSms.setUserPhone(userPhone);
                userSms.setStatus(false);

                log.info("准备为用户 {} 更新数据库状态为离线...", userPhone);
                userSmsWebSocketService.saveUserWsaocket(userSms); // 更新用户为离线状态
                log.info("成功更新用户 {} 的数据库状态为离线。", userPhone);
                log.info("用户 {} 的WebSocket连接已断开, SessionId: {}", userPhone, session.getId());

            } catch (Exception e) {
                // 捕获所有异常，并使用日志框架记录下来，e 参数会打印堆栈信息
                log.error("为用户 {} 更新离线状态时发生数据库异常! SessionId: {}", userPhone, session.getId(), e);
            }
        } else {
            log.error("一个未注册的WebSocket连接已断开, SessionId: {}", session.getId());
        }
    }

    /**
     * 封装发送消息的方法，自动将Result对象转为JSON字符串
     */
    private void sendMessage(WebSocketSession session, Result<?> result) throws IOException {
        if (session != null && session.isOpen()) {
            session.sendMessage(new TextMessage(JSONUtil.toJsonStr(result)));
        }
    }
}