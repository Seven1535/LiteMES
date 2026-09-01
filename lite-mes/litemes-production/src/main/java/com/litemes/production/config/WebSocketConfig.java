package com.litemes.production.config;

import com.litemes.production.websocket.ProductionWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置：/ws/production 实时推送生产事件（见《架构设计说明书》3.6）。
 * 握手经网关鉴权（Token 放 query 参数），前端统一走 utils/websocket.js 封装。
 * Handler 为 @Component 单例（事件发布器注入同一实例进行广播）。
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
