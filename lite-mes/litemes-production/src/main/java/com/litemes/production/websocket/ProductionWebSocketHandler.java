package com.litemes.production.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 生产事件 WebSocket 端点处理器（/ws/production，见《架构设计说明书》3.6）：
 * 连接成功推送 CONNECTED 确认；收到 ping 回 pong（心跳保活）；业务事件经 broadcast 广播给所有在线看板。
 */
@Slf4j
@Component
public class ProductionWebSocketHandler extends TextWebSocketHandler {

    private static final String PING = "ping";
    private static final String PONG = "pong";

    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    private final ObjectMapper objectMapper;

    public ProductionWebSocketHandler() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        // 连接成功确认（前端据此标记在线状态）
        String json = objectMapper.writeValueAsString(
                ProductionEvent.of("CONNECTED", Map.of("sessions", sessions.size())));
        session.sendMessage(new TextMessage(json));
        log.info("看板 WebSocket 连接建立: {}，当前在线 {}", session.getId(), sessions.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 心跳保活：客户端每 30s 发 ping，服务端回 pong
        if (PING.equals(message.getPayload())) {
            session.sendMessage(new TextMessage(PONG));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("看板 WebSocket 连接关闭: {}（{}），当前在线 {}", session.getId(), status, sessions.size());
    }

    /** 广播事件给所有在线会话；单个会话发送失败只清理该会话，不影响其他看板 */
    public void broadcast(ProductionEvent event) {
        if (sessions.isEmpty()) {
            return;
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            log.warn("WebSocket 事件序列化失败（{}）: {}", event.getType(), e.getMessage());
            return;
        }
        for (WebSocketSession session : sessions) {
            try {
                if (session.isOpen()) {
                    synchronized (session) {
                        session.sendMessage(new TextMessage(json));
                    }
                } else {
                    sessions.remove(session);
                }
            } catch (Exception e) {
                log.warn("WebSocket 推送失败（会话 {}）: {}", session.getId(), e.getMessage());
                sessions.remove(session);
            }
        }
    }
}
