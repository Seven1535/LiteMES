package com.litemes.production.module.dispatch.service;

import com.litemes.api.base.UserClient;
import com.litemes.api.base.WorkCenterClient;
import com.litemes.api.base.WorkflowStepClient;
import com.litemes.api.base.dto.UserDTO;
import com.litemes.api.base.dto.WorkCenterDTO;
import com.litemes.api.base.dto.WorkflowStepDTO;
import com.litemes.common.code.CodeGenerator;
import com.litemes.common.core.BusinessException;
import com.litemes.common.core.PageResult;
import com.litemes.production.module.dispatch.dto.DispatchTaskCreateRequest;
import com.litemes.production.module.dispatch.dto.DispatchTaskQueryRequest;
import com.litemes.production.module.dispatch.dto.DispatchTaskVO;
import com.litemes.production.module.dispatch.entity.DispatchTask;
import com.litemes.production.module.dispatch.repository.DispatchTaskRepository;
import com.litemes.production.module.workorder.entity.WorkOrder;
import com.litemes.production.module.workorder.repository.WorkOrderRepository;
import com.litemes.production.module.workorder.service.WorkOrderService;
import com.litemes.production.websocket.ProductionEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 派工任务业务：派工（校验工位/操作员/可派量）/ 开工 / 报工（Redisson 分布式锁防并发）。
 * 任务状态机见《设计规格说明书》4.3：PENDING →（开工）→ PROCESSING →（报工累计满）→ COMPLETED。
 * 状态联动（4.2 / 8.3）：
 * - 已下达工单首次派工 → IN_PROGRESS（轻量实现：有派工任务即视为投产）；
 * - 开工时工位置 BUSY、写工单实际开始日期；任务完成后工位无进行中任务则回 IDLE；
 * - 报工累计工单已完工数量，工单下全部任务完成 → 工单 COMPLETED。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DispatchTaskService {

    private static final String DEL_FLAG_NORMAL = "0";

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CLOSED = "CLOSED";

    private static final String WORK_CENTER_BUSY = "BUSY";
    private static final String WORK_CENTER_IDLE = "IDLE";
    private static final String WORK_CENTER_OFFLINE = "OFFLINE";

    private final DispatchTaskRepository dispatchTaskRepository;
    private final WorkOrderRepository workOrderRepository;
    private final CodeGenerator codeGenerator;
    private final RedissonClient redissonClient;
    private final PlatformTransactionManager transactionManager;
    private final WorkflowStepClient workflowStepClient;
    private final WorkCenterClient workCenterClient;
    private final UserClient userClient;
    private final ProductionEventPublisher eventPublisher;

    /** 分页查询：工单/工位/操作员三选一（工单优先）+ 状态精确；名称字段经 Feign 聚合 */
    public PageResult<DispatchTaskVO> page(DispatchTaskQueryRequest query) {
        Pageable pageable = PageRequest.of(query.getPageNum() - 1, query.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        String workOrderId = trimToNull(query.getWorkOrderId());
        String workCenterId = trimToNull(query.getWorkCenterId());
        String operatorId = trimToNull(query.getOperatorId());
        String status = trimToNull(query.getStatus());

        Page<DispatchTask> page;
        if (workOrderId != null) {
            page = status != null
                    ? dispatchTaskRepository.findByDelFlagAndWorkOrderIdAndStatus(DEL_FLAG_NORMAL, workOrderId, status, pageable)
                    : dispatchTaskRepository.findByDelFlagAndWorkOrderId(DEL_FLAG_NORMAL, workOrderId, pageable);
        } else if (workCenterId != null) {
            page = status != null
                    ? dispatchTaskRepository.findByDelFlagAndWorkCenterIdAndStatus(DEL_FLAG_NORMAL, workCenterId, status, pageable)
                    : dispatchTaskRepository.findByDelFlagAndWorkCenterId(DEL_FLAG_NORMAL, workCenterId, pageable);
        } else if (operatorId != null) {
            page = status != null
                    ? dispatchTaskRepository.findByDelFlagAndOperatorIdAndStatus(DEL_FLAG_NORMAL, operatorId, status, pageable)
                    : dispatchTaskRepository.findByDelFlagAndOperatorId(DEL_FLAG_NORMAL, operatorId, pageable);
        } else {
            page = dispatchTaskRepository.findByDelFlag(DEL_FLAG_NORMAL, pageable);
        }

        Map<String, String> orderNoCache = new HashMap<>();
        Map<String, WorkflowStepDTO> stepCache = new HashMap<>();
        Map<String, WorkCenterDTO> centerCache = new HashMap<>();
        Map<String, UserDTO> userCache = new HashMap<>();
        return PageResult.of(page.getContent().stream()
                        .map(task -> toVO(task, orderNoCache, stepCache, centerCache, userCache)).toList(),
                page.getTotalElements(), query.getPageNum(), query.getPageSize());
    }

    public DispatchTaskVO getById(String id) {
        return toVO(loadActive(id), new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>());
    }

    /**
     * 派工（业务规则 8.3）：
     * 1. 工单必须已下达/生产中；2. 工序必须属于工单锁定的工艺版本；
     * 3. 工位存在且未停用（OFFLINE）；4. 操作员存在且启用；
     * 5. 该工序累计派工量（不含已关闭）+ 本次 ≤ 工单计划数量（同一工序可拆分派到不同工位）。
     */
    @Transactional
    public DispatchTaskVO create(DispatchTaskCreateRequest request) {
        WorkOrder order = loadWorkOrder(request.getWorkOrderId());
        if (!WorkOrderService.STATUS_RELEASED.equals(order.getStatus())
                && !WorkOrderService.STATUS_IN_PROGRESS.equals(order.getStatus())) {
            throw new BusinessException(400, "仅已下达/生产中的工单可派工");
        }

        // 工序必须属于工单锁定的工艺版本（强一致校验，base 不可用时拒绝派工）
        List<WorkflowStepDTO> steps = workflowStepClient.listByWorkflow(order.getWorkflowId());
        boolean stepValid = steps.stream().anyMatch(s -> s.getId().equals(request.getWorkflowStepId()));
        if (!stepValid) {
            throw new BusinessException(400, "所选工序不属于该工单锁定的工艺版本");
        }

        WorkCenterDTO workCenter = workCenterClient.getWorkCenter(request.getWorkCenterId());
        if (WORK_CENTER_OFFLINE.equals(workCenter.getStatus())) {
            throw new BusinessException(400, "工位已停用（OFFLINE），不能派工");
        }

        UserDTO operator = userClient.getUser(request.getOperatorId());
        if (!"ENABLED".equals(operator.getStatus())) {
            throw new BusinessException(400, "操作员账号已停用，不能派工");
        }

        long dispatched = dispatchTaskRepository.sumDispatchQty(order.getId(), request.getWorkflowStepId());
        if (dispatched + request.getQuantity() > order.getQuantity()) {
            throw new BusinessException(400, "该工序已派工 " + dispatched + "，工单计划数量 "
                    + order.getQuantity() + "，本次派工数量超出上限");
        }

        DispatchTask task = new DispatchTask();
        task.setId(UUID.randomUUID().toString());
        task.setTaskNo(codeGenerator.generateDispatchTaskNo());
        task.setWorkOrderId(order.getId());
        task.setWorkflowStepId(request.getWorkflowStepId());
        task.setWorkCenterId(request.getWorkCenterId());
        task.setOperatorId(request.getOperatorId());
        task.setQuantity(request.getQuantity());
        task.setCompletedQty(0);
        task.setStatus(STATUS_PENDING);
        task.setDispatchedAt(LocalDateTime.now());
        task.setRemark(trimToNull(request.getRemark()));
        DispatchTask saved = dispatchTaskRepository.save(task);

        // 状态联动：已下达工单首次派工 → 生产中（4.2，轻量实现：有派工任务即视为投产）
        if (WorkOrderService.STATUS_RELEASED.equals(order.getStatus())) {
            order.setStatus(WorkOrderService.STATUS_IN_PROGRESS);
            workOrderRepository.save(order);
            eventPublisher.publishOrderStatusChanged(order);
        }
        return toVO(saved, Map.of(order.getId(), order.getOrderNo()),
                new HashMap<>(), new HashMap<>(), new HashMap<>());
    }

    /** 开工：仅待开工可开工（4.3）；工位置 BUSY（弱依赖，失败不阻断）、写工单实际开始日期 */
    @Transactional
    public DispatchTaskVO start(String id) {
        DispatchTask task = loadActive(id);
        if (!STATUS_PENDING.equals(task.getStatus())) {
            throw new BusinessException(400, "仅待开工状态的任务可开工");
        }
        task.setStatus(STATUS_PROCESSING);
        task.setStartedAt(LocalDateTime.now());
        DispatchTask saved = dispatchTaskRepository.save(task);

        changeWorkCenterStatusQuietly(task.getWorkCenterId(), WORK_CENTER_BUSY);
        eventPublisher.publishTaskStarted(saved);
        eventPublisher.publishWorkCenterStatusChanged(task.getWorkCenterId(), WORK_CENTER_BUSY);

        WorkOrder order = loadWorkOrder(task.getWorkOrderId());
        if (order.getActualStartDate() == null) {
            order.setActualStartDate(LocalDate.now());
            workOrderRepository.save(order);
        }
        return toVO(saved, Map.of(order.getId(), order.getOrderNo()),
                new HashMap<>(), new HashMap<>(), new HashMap<>());
    }

    /**
     * 报工（8.3）：仅进行中任务可报工；报工数量 ≤ 任务剩余未完成数量；
     * 累计满 → 任务自动完成；工单已完工数量同步累计，全部任务完成 → 工单完成。
     * Redisson 分布式锁防并发报工（锁必须包住事务：提交后再释放，避免并发读到旧完成量）。
     */
    public DispatchTaskVO report(String id, int quantity) {
        RLock lock = redissonClient.getLock("dispatch:task:report:" + id);
        boolean locked;
        try {
            locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(500, "报工被中断，请重试");
        }
        if (!locked) {
            throw new BusinessException(409, "该任务正在报工中，请稍后重试");
        }
        try {
            return new TransactionTemplate(transactionManager)
                    .execute(status -> doReport(id, quantity));
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /** 报工核心逻辑（在分布式锁内、独立事务中执行） */
    private DispatchTaskVO doReport(String id, int quantity) {
        DispatchTask task = loadActive(id);
        if (!STATUS_PROCESSING.equals(task.getStatus())) {
            throw new BusinessException(400, "仅进行中状态的任务可报工");
        }
        int remaining = task.getQuantity() - task.getCompletedQty();
        if (quantity > remaining) {
            throw new BusinessException(400, "报工数量超出任务剩余未完成数量（剩余 " + remaining + "）");
        }

        boolean taskFinished = task.getCompletedQty() + quantity == task.getQuantity();
        task.setCompletedQty(task.getCompletedQty() + quantity);
        if (taskFinished) {
            task.setStatus(STATUS_COMPLETED);
            task.setCompletedAt(LocalDateTime.now());
        }
        DispatchTask saved = dispatchTaskRepository.save(task);

        // 工单联动：累计已完工数量；全部任务完成 → 工单完成（4.2）
        WorkOrder order = loadWorkOrder(task.getWorkOrderId());
        order.setCompletedQty(order.getCompletedQty() + quantity);
        boolean allTasksDone = dispatchTaskRepository.findByDelFlagAndWorkOrderId(DEL_FLAG_NORMAL, order.getId())
                .stream().allMatch(t -> STATUS_COMPLETED.equals(t.getStatus()) || STATUS_CLOSED.equals(t.getStatus()));
        if (allTasksDone && !WorkOrderService.STATUS_COMPLETED.equals(order.getStatus())) {
            order.setStatus(WorkOrderService.STATUS_COMPLETED);
            order.setActualEndDate(LocalDate.now());
        }
        workOrderRepository.save(order);
        eventPublisher.publishOrderStatusChanged(order);

        // 工位联动：任务完成且该工位无其他进行中任务 → 回 IDLE（弱依赖）
        if (taskFinished) {
            if (dispatchTaskRepository.countByDelFlagAndWorkCenterIdAndStatus(
                    DEL_FLAG_NORMAL, task.getWorkCenterId(), STATUS_PROCESSING) == 0) {
                changeWorkCenterStatusQuietly(task.getWorkCenterId(), WORK_CENTER_IDLE);
                eventPublisher.publishWorkCenterStatusChanged(task.getWorkCenterId(), WORK_CENTER_IDLE);
            }
            eventPublisher.publishTaskCompleted(saved, order);
        }
        return toVO(saved, Map.of(order.getId(), order.getOrderNo()),
                new HashMap<>(), new HashMap<>(), new HashMap<>());
    }

    /**
     * 工单关闭级联：未完成任务（PENDING/PROCESSING）一并关闭，保持状态一致；
     * 被级联关闭的进行中任务，其工位无其他进行中任务时回 IDLE。
     */
    @Transactional
    public int closeByWorkOrder(String workOrderId) {
        List<DispatchTask> openTasks = dispatchTaskRepository.findByDelFlagAndWorkOrderIdAndStatusIn(
                DEL_FLAG_NORMAL, workOrderId, List.of(STATUS_PENDING, STATUS_PROCESSING));
        for (DispatchTask task : openTasks) {
            boolean wasProcessing = STATUS_PROCESSING.equals(task.getStatus());
            task.setStatus(STATUS_CLOSED);
            dispatchTaskRepository.save(task);
            eventPublisher.publishTaskClosed(task);
            if (wasProcessing && dispatchTaskRepository.countByDelFlagAndWorkCenterIdAndStatus(
                    DEL_FLAG_NORMAL, task.getWorkCenterId(), STATUS_PROCESSING) == 0) {
                changeWorkCenterStatusQuietly(task.getWorkCenterId(), WORK_CENTER_IDLE);
                eventPublisher.publishWorkCenterStatusChanged(task.getWorkCenterId(), WORK_CENTER_IDLE);
            }
        }
        return openTasks.size();
    }

    /** 工位状态变更弱依赖：base 服务不可用时仅告警，不阻断生产核心流程 */
    private void changeWorkCenterStatusQuietly(String workCenterId, String status) {
        try {
            workCenterClient.changeStatus(workCenterId, status);
        } catch (Exception e) {
            log.warn("更新工位状态失败（工位 {} → {}）: {}", workCenterId, status, e.getMessage());
        }
    }

    private DispatchTask loadActive(String id) {
        return dispatchTaskRepository.findById(id)
                .filter(t -> DEL_FLAG_NORMAL.equals(t.getDelFlag()))
                .orElseThrow(() -> new BusinessException(404, "派工任务不存在"));
    }

    private WorkOrder loadWorkOrder(String workOrderId) {
        return workOrderRepository.findById(workOrderId)
                .filter(o -> DEL_FLAG_NORMAL.equals(o.getDelFlag()))
                .orElseThrow(() -> new BusinessException(404, "工单不存在"));
    }

    private DispatchTaskVO toVO(DispatchTask task, Map<String, String> orderNoCache,
                                Map<String, WorkflowStepDTO> stepCache,
                                Map<String, WorkCenterDTO> centerCache,
                                Map<String, UserDTO> userCache) {
        DispatchTaskVO vo = new DispatchTaskVO();
        vo.setId(task.getId());
        vo.setTaskNo(task.getTaskNo());
        vo.setWorkOrderId(task.getWorkOrderId());
        vo.setWorkflowStepId(task.getWorkflowStepId());
        vo.setWorkCenterId(task.getWorkCenterId());
        vo.setOperatorId(task.getOperatorId());
        vo.setQuantity(task.getQuantity());
        vo.setCompletedQty(task.getCompletedQty());
        vo.setStatus(task.getStatus());
        vo.setDispatchedAt(task.getDispatchedAt());
        vo.setStartedAt(task.getStartedAt());
        vo.setCompletedAt(task.getCompletedAt());
        vo.setRemark(task.getRemark());
        vo.setCreatedAt(task.getCreatedAt());
        vo.setUpdatedAt(task.getUpdatedAt());

        try {
            vo.setWorkOrderNo(orderNoCache.computeIfAbsent(task.getWorkOrderId(), oid ->
                    workOrderRepository.findById(oid).map(WorkOrder::getOrderNo).orElse(null)));
        } catch (Exception e) {
            log.warn("聚合工单编号失败（任务 {}）: {}", task.getTaskNo(), e.getMessage());
        }
        // Feign 聚合：工序/工位/操作员查询失败不阻断列表展示（仅冗余字段缺失）
        try {
            WorkflowStepDTO step = stepCache.computeIfAbsent(task.getWorkflowStepId(), workflowStepClient::getStep);
            vo.setStepName(step.getStepName());
        } catch (Exception e) {
            log.warn("聚合工序信息失败（任务 {}）: {}", task.getTaskNo(), e.getMessage());
        }
        try {
            WorkCenterDTO center = centerCache.computeIfAbsent(task.getWorkCenterId(), workCenterClient::getWorkCenter);
            vo.setWorkCenterName(center.getCenterName());
        } catch (Exception e) {
            log.warn("聚合工位信息失败（任务 {}）: {}", task.getTaskNo(), e.getMessage());
        }
        try {
            UserDTO operator = userCache.computeIfAbsent(task.getOperatorId(), userClient::getUser);
            vo.setOperatorName(operator.getRealName());
        } catch (Exception e) {
            log.warn("聚合操作员信息失败（任务 {}）: {}", task.getTaskNo(), e.getMessage());
        }
        return vo;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
