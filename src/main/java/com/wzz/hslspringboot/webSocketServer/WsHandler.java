package com.wzz.hslspringboot.webSocketServer;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.wzz.hslspringboot.DTO.Result;
import com.wzz.hslspringboot.modules.ServiceExecutor;
import com.wzz.hslspringboot.pojo.UserSmsWebSocket;
import com.wzz.hslspringboot.service.UserSmsWebSocketService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.alibaba.fastjson.JSONObject;
@Component
public class WsHandler  extends TextWebSocketHandler {

    @Autowired
    private  ObjectMapper objectMapper;
    @Autowired
    private UserSmsWebSocketService userSmsWebSocketService;
    static Map<String, WebSocketSession> wssessionMap = new ConcurrentHashMap<>();
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        wssessionMap.put(session.getId(), session);
        System.out.println("WebSocket is open with sessionId: " + session.getId());
    }
    @Override
    protected void handleTextMessage(WebSocketSession session, org.springframework.web.socket.TextMessage message) throws Exception {
        // 收到消息时的处理逻辑
        System.out.println("收到消息: " + message.getPayload());
        //使用dto解析message.getPayload()
//        MessageDTO messageDTO = objectMapper.readValue(payload, MessageDTO.class);

//        System.out.println("解析后的 DTO: " + messageDTO);


//        JSONObject jsonObject = JSONObject.parseObject(message.getPayload());
//        if (jsonObject.equals("service")||jsonObject.equals("method")) {
//            session.sendMessage(new org.springframework.web.socket.TextMessage(Result.error("未指定类和方法").toString()));
//        }
//
//        Object re= ServiceExecutor.dispatch(jsonObject.getString("service"), jsonObject.getString("method"), jsonObject.getString("ages"));
//        JSONObject json = (JSONObject) JSONObject.toJSON(Result.success(re));
//        session.sendMessage(new org.springframework.web.socket.TextMessage(json.toString()));

//        BeanUtils.copyProperties();//dto/pojo
    }
    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
        wssessionMap.remove(session.getId());
        System.out.println("WebSocket 已断开连接: " + session.getId());
    }
}
