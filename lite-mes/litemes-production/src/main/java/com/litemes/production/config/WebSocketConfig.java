package com.litemes.production.config;

import com.litemes.production.websocket.ProductionWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置：/ws/production 实时推送生产事件（见《架构设计说明书》3.6）。
 * 握手经网关鉴权（Token 放 query 参数），前端统一走 utils/websocket.js 封装。
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(productionWebSocketHandler(), "/ws/production")
                .setAllowedOrigins("*");
    }

    @Bean
    public ProductionWebSocketHandler productionWebSocketHandler() {
        return new ProductionWebSocketHandler();
    }
}
