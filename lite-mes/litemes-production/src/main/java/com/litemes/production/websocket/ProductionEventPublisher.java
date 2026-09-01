package com.litemes.production.websocket;

import com.litemes.production.module.dispatch.entity.DispatchTask;
import com.litemes.production.module.workorder.entity.WorkOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 生产事件统一发布器：业务状态流转点调用此处发布事件（架构设计 3.6 事件表）。
 * 所有发布均为弱依赖——推送失败仅告警，绝不影响业务主流程。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductionEventPublisher {

    private static final String TASK_STARTED = "TASK_STARTED";
    private static final String TASK_COMPLETED = "TASK_COMPLETED";
    private static final String TASK_CLOSED = "TASK_CLOSED";
    private static final String ORDER_STATUS_CHANGED = "ORDER_STATUS_CHANGED";
    private static final String WORKCENTER_STATUS_CHANGED = "WORKCENTER_STATUS_CHANGED";

    private final ProductionWebSocketHandler webSocketHandler;

    /** 任务开工（携带工位信息，看板刷新工位负载） */
    public void publishTaskStarted(DispatchTask task) {
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", task.getId());
        data.put("taskNo", task.getTaskNo());
        data.put("workOrderId", task.getWorkOrderId());
        data.put("workCenterId", task.getWorkCenterId());
        publish(TASK_STARTED, data);
    }

    /** 任务自动完成（携带完工数量与工单进度） */
    public void publishTaskCompleted(DispatchTask task, WorkOrder order) {
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", task.getId());
        data.put("taskNo", task.getTaskNo());
        data.put("workOrderId", order.getId());
        data.put("completedQty", task.getCompletedQty());
        data.put("orderCompletedQty", order.getCompletedQty());
        data.put("orderQuantity", order.getQuantity());
        data.put("progress", order.getQuantity() == 0 ? 0
                : Math.min(100, order.getCompletedQty() * 100 / order.getQuantity()));
        publish(TASK_COMPLETED, data);
    }

    /** 工单关闭导致任务级联关闭 */
    public void publishTaskClosed(DispatchTask task) {
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", task.getId());
        data.put("taskNo", task.getTaskNo());
        data.put("workOrderId", task.getWorkOrderId());
        publish(TASK_CLOSED, data);
    }

    /** 工单状态变更 */
    public void publishOrderStatusChanged(WorkOrder order) {
        Map<String, Object> data = new HashMap<>();
        data.put("workOrderId", order.getId());
        data.put("orderNo", order.getOrderNo());
        data.put("status", order.getStatus());
        publish(ORDER_STATUS_CHANGED, data);
    }

    /** 工位状态变更（派工开工/完工联动） */
    public void publishWorkCenterStatusChanged(String workCenterId, String status) {
        Map<String, Object> data = new HashMap<>();
        data.put("workCenterId", workCenterId);
        data.put("status", status);
        publish(WORKCENTER_STATUS_CHANGED, data);
    }

    private void publish(String type, Map<String, Object> data) {
        try {
            webSocketHandler.broadcast(ProductionEvent.of(type, data));
        } catch (Exception e) {
            log.warn("发布事件 {} 失败: {}", type, e.getMessage());
        }
    }
}
