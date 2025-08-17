package com.wzz.hslspringboot.config;

import com.wzz.hslspringboot.webSocketServer.WsHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private static final Logger log = LogManager.getLogger(WebSocketConfig.class);
    // 注入由Spring管理的WsHandler Bean
    @Autowired
    private WsHandler wsHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        log.info("注册 WebSocket 处理器...");
        // 使用注入的 wsHandler 实例
        registry.addHandler(wsHandler, "/ws")
                .setAllowedOrigins("*");
        log.info("WebSocket 地址为：/ws");
    }
}