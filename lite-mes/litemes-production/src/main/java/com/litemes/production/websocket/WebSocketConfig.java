package com.litemes.production.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 注册：端点 /ws/production（网关按 /ws/** 路由转发，Token 经 query 参数过网关鉴权）。
 * 允许跨域来源：本地开发 5173 与生产 Nginx 均经网关代理，来源统一为网关入口。
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final ProductionWebSocketHandler productionWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(productionWebSocketHandler, "/ws/production")
                .setAllowedOriginPatterns("*");
    }
}
