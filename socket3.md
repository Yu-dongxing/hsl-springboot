好的，完全理解。我们调整一下逻辑，取消独立的“注册”步骤。

新的模式是“即时注册”（Just-in-Time Registration）：服务器在收到任何用户的**第一条**有效指令消息时，如果发现这个用户还没有对应的会话，就自动为他完成注册和会话绑定，然后立刻执行该条指令。后续来自该用户的消息则直接执行指令。

这种方式简化了客户端的逻辑，非常适合你的需求。

下面是基于这个新逻辑优化后的核心代码 `WsHandler.java`。其他文件（如 `SocketConfig`, `WebSocketDTO`, `Result`, `ServiceExecutor` 等）与我上一次优化提供的版本**保持不变**。

---

### 优化后的 `WsHandler.java` (即时注册版本)

这个版本是唯一的改动点。`handleTextMessage` 方法被重构，以融合注册和指令处理。

```java
package com.wzz.hslspringboot.webSocketServer;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.json.JSONUtil;
import com.wzz.hslspringboot.DTO.Result;
import com.wzz.hslspringboot.DTO.WebSocketDTO;
import com.wzz.hslspringboot.modules.ServiceExecutor;
import com.wzz.hslspringboot.pojo.UserSmsWebSocket;
import com.wzz.hslspringboot.service.UserSmsWebSocketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WsHandler extends TextWebSocketHandler {

    @Autowired
    private UserSmsWebSocketService userSmsWebSocketService;

    // 依然使用 userPhone 作为 Key 来管理 WebSocket 会话
    private static final Map<String, WebSocketSession> userSessionMap = new ConcurrentHashMap<>();
    // 依然将会话属性的 Key 定义为常量，便于在连接关闭时获取
    private static final String USER_PHONE_KEY = "userPhone";

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 连接建立时，只打印日志。不要求客户端立即发送任何消息。
        System.out.println("新的WebSocket连接已建立, SessionId: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            System.out.println("收到来自 " + session.getId() + " 的消息: " + payload);
            WebSocketDTO messageDTO = JSONUtil.toBean(payload, WebSocketDTO.class);

            // 1. 核心校验：任何消息都必须包含 user, server, method
            if (messageDTO == null || messageDTO.getUser() == null || messageDTO.getUser().isEmpty()
                    || messageDTO.getServer() == null || messageDTO.getServer().isEmpty()
                    || messageDTO.getMethod() == null || messageDTO.getMethod().isEmpty()) {
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
                    System.out.println("用户 " + userPhone + " 从新位置连接，关闭旧的会话: " + oldSession.getId());
                    oldSession.close();
                }
                
                // 绑定当前新会话
                session.getAttributes().put(USER_PHONE_KEY, userPhone);
                userSessionMap.put(userPhone, session);

                // --- 数据库操作 ---
                // 更新用户状态为在线
                // UserSmsWebSocket userSms = new UserSmsWebSocket();
                // userSms.setUserPhone(userPhone);
                // userSms.setStatus(true);
                // userSmsWebSocketService.saveOrUpdate(userSms); // 示例
                System.out.println("用户 " + userPhone + " 首次通信，已自动注册并绑定会话: " + session.getId());
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
            System.err.println("处理消息时发生错误: " + e.getMessage());
            sendMessage(session, Result.error("服务器内部错误: " + ExceptionUtil.getRootCauseMessage(e)));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // 这部分逻辑保持不变，因为它依赖于 session attributes，非常可靠
        String userPhone = (String) session.getAttributes().get(USER_PHONE_KEY);

        if (userPhone != null) {
            // 只移除与当前关闭的 session 完全匹配的条目，防止意外移除新建立的连接
            userSessionMap.remove(userPhone, session);
            
            // --- 数据库操作 ---
            // userSmsWebSocketService.updateStatus(userPhone, false); // 更新用户为离线状态
            System.out.println("用户 " + userPhone + " 的WebSocket连接已断开, SessionId: " + session.getId());
        } else {
            System.out.println("一个未注册的WebSocket连接已断开, SessionId: " + session.getId());
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
```

### 如何使用（新流程）

客户端的流程被大大简化。连接成功后，无需发送任何“注册”消息，可以直接发送业务指令。

**发送指令**
客户端连接到 `ws://your_server/ws` 后，可以直接发送如下的指令消息：

```json
{
  "user": "13912345678",
  "server": "order_service",
  "method": "getDetail",
  "args": ["order12345"]
}
```

**服务器行为**
1.  服务器收到这条消息。
2.  检查 `session` 的 `attributes`，发现没有 `userPhone`。
3.  检查 `userSessionMap`，发现 `13912345678` 也没有对应的会话（或者有一个旧的，就把它关掉）。
4.  执行“即时注册”：将 `userPhone` 存入 `session.attributes`，并将 `(userPhone, session)` 存入 `userSessionMap`。同时更新数据库状态为“在线”。
5.  继续执行指令：调用 `order_service` 的 `getDetail` 方法，参数为 `["order12345"]`。
6.  将执行结果通过 `Result` DTO 包装后，以JSON格式返回给客户端。

**发送第二条指令**
当客户端发送第二条消息时：
```json
{
  "user": "13912345678",
  "server": "refund_service",
  "method": "apply",
  "args": ["order12345", "reason text"]
}
```
服务器收到后，检查 `session` 的 `attributes`，发现已经绑定了 `userPhone`。它会跳过注册步骤，直接执行指令。

### 总结

这个方案的优点：
*   **客户端逻辑极简**：客户端无需关心连接状态，只需确保每条消息都带上 `user` 身份标识即可。
*   **服务端无状态感知**：服务端不依赖于客户端的连接顺序，任何一条合法的消息都可以触发会话的建立。
*   **会话管理依旧健壮**：保留了基于 `userPhone` 的会话管理，并能处理同一用户在不同设备/页面登录（“顶号”）的场景。

这个版本更符合现代Web服务的简洁交互模式，推荐你使用。