package com.litemes.production.websocket;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * WebSocket 推送事件（事件类型见《架构设计说明书》3.6）：
 * TASK_STARTED / TASK_COMPLETED / TASK_CLOSED / ORDER_STATUS_CHANGED / WORKCENTER_STATUS_CHANGED。
 */
@Data
public class ProductionEvent {

    /** 事件类型（与前端 utils/constants.js 的 WS_EVENT 一致） */
    private String type;

    /** 事件负载（taskId/workOrderId/progress 等，按事件类型而定） */
    private Map<String, Object> data;

    /** 事件产生时间 */
    private LocalDateTime timestamp;

    public static ProductionEvent of(String type, Map<String, Object> data) {
        ProductionEvent event = new ProductionEvent();
        event.setType(type);
        event.setData(data);
        event.setTimestamp(LocalDateTime.now());
        return event;
    }
}
