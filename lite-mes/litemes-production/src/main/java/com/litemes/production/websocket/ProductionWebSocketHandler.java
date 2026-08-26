package com.litemes.production.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 生产事件 WebSocket 处理器（骨架实现）。
 * 事件类型见《架构设计说明书》3.6：TASK_STARTED / TASK_COMPLETED / TASK_CLOSED /
 * ORDER_STATUS_CHANGED / WORKCENTER_STATUS_CHANGED。
 * 连接管理：断线由前端重连（指数退避），重连成功后前端全量拉取一次数据。
 */
@Slf4j
public class ProductionWebSocketHandler extends TextWebSocketHandler {

    /** 在线连接集合（单实例部署，线程安全） */
    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("看板 WS 连接建立: {}, 当前在线: {}", session.getId(), sessions.size());
        // 连接成功确认
        send(session, "{\"type\":\"CONNECTED\"}");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("看板 WS 连接关闭: {}, 当前在线: {}", session.getId(), sessions.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // TODO: 处理客户端消息（如心跳 pong），当前 MVP 以服务端单向推送为主
    }

    /**
     * 广播生产事件给所有看板连接。
     * TODO: 由业务模块（报工/工单状态流转/工位状态变更）触发调用。
     */
    public void broadcast(String eventJson) {
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                send(session, eventJson);
            }
        }
    }

    private void send(WebSocketSession session, String payload) {
        try {
            session.sendMessage(new TextMessage(payload));
        } catch (IOException e) {
            log.warn("WS 消息发送失败: {}", session.getId(), e);
        }
    }
}
