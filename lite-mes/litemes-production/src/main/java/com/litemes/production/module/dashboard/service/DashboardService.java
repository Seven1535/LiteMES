package com.litemes.production.module.dashboard.service;

import com.litemes.api.base.ProductClient;
import com.litemes.api.base.UserClient;
import com.litemes.api.base.WorkCenterClient;
import com.litemes.api.base.WorkflowStepClient;
import com.litemes.api.base.dto.ProductDTO;
import com.litemes.api.base.dto.WorkCenterDTO;
import com.litemes.production.module.dashboard.dto.DashboardSummaryVO;
import com.litemes.production.module.dashboard.dto.WorkCenterLoadVO;
import com.litemes.production.module.dashboard.dto.WorkOrderProgressVO;
import com.litemes.production.module.dispatch.entity.DispatchTask;
import com.litemes.production.module.dispatch.repository.DispatchTaskRepository;
import com.litemes.production.module.dispatch.service.DispatchTaskService;
import com.litemes.production.module.workorder.entity.WorkOrder;
import com.litemes.production.module.workorder.repository.WorkOrderRepository;
import com.litemes.production.module.workorder.service.WorkOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 看板聚合服务：聚合本服务业务数据 + Feign 主数据（设计规格 6.3.2 / 7.1 看板模块）。
 * 概览数据走 Redis 缓存（dashboard:summary，TTL 30s，见《开发规范说明文档》3.8）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final String DEL_FLAG_NORMAL = "0";
    private static final String SUMMARY_CACHE_KEY = "dashboard:summary";
    private static final long SUMMARY_CACHE_SECONDS = 30;

    private final WorkOrderRepository workOrderRepository;
    private final DispatchTaskRepository dispatchTaskRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ProductClient productClient;
    private final WorkCenterClient workCenterClient;
    private final WorkflowStepClient workflowStepClient;
    private final UserClient userClient;

    /** 概览：工单统计 + 工位状态统计 + 今日产出（30s 缓存，缓存失败不阻断） */
    public DashboardSummaryVO summary() {
        try {
            Object cached = redisTemplate.opsForValue().get(SUMMARY_CACHE_KEY);
            if (cached instanceof DashboardSummaryVO vo) {
                return vo;
            }
        } catch (Exception e) {
            log.warn("读取看板缓存失败: {}", e.getMessage());
        }

        DashboardSummaryVO vo = buildSummary();
        try {
            redisTemplate.opsForValue().set(SUMMARY_CACHE_KEY, vo, SUMMARY_CACHE_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("写入看板缓存失败: {}", e.getMessage());
        }
        return vo;
    }

    /** 在制工单进度列表（已下达 + 生产中，优先级升序；产品名称 Feign 聚合） */
    public List<WorkOrderProgressVO> workOrderProgress() {
        List<WorkOrder> orders = workOrderRepository.findByDelFlagAndStatusInOrderByPriorityAscCreatedAtDesc(
                DEL_FLAG_NORMAL, List.of(WorkOrderService.STATUS_RELEASED, WorkOrderService.STATUS_IN_PROGRESS));
        Map<String, ProductDTO> productCache = new HashMap<>();
        List<WorkOrderProgressVO> result = new ArrayList<>();
        for (WorkOrder order : orders) {
            WorkOrderProgressVO vo = new WorkOrderProgressVO();
            vo.setId(order.getId());
            vo.setOrderNo(order.getOrderNo());
            vo.setQuantity(order.getQuantity());
            vo.setCompletedQty(order.getCompletedQty());
            vo.setPercentage(order.getQuantity() == 0 ? 0
                    : Math.min(100, order.getCompletedQty() * 100 / order.getQuantity()));
            vo.setPriority(order.getPriority());
            vo.setStatus(order.getStatus());
            try {
                ProductDTO product = productCache.computeIfAbsent(order.getProductId(), productClient::getProduct);
                vo.setProductName(product.getProductName());
            } catch (Exception e) {
                log.warn("看板聚合产品信息失败（工单 {}）: {}", order.getOrderNo(), e.getMessage());
            }
            result.add(vo);
        }
        return result;
    }

    /** 工位负载列表：全量工位（Feign）+ 各自当前进行中任务（无任务即空闲） */
    public List<WorkCenterLoadVO> workCenterLoad() {
        List<WorkCenterDTO> centers = workCenterClient.listWorkCenters(null);
        List<WorkCenterLoadVO> result = new ArrayList<>();
        for (WorkCenterDTO center : centers) {
            WorkCenterLoadVO vo = new WorkCenterLoadVO();
            vo.setId(center.getId());
            vo.setCenterCode(center.getCenterCode());
            vo.setCenterName(center.getCenterName());
            vo.setStatus(center.getStatus());

            List<DispatchTask> tasks = dispatchTaskRepository.findByDelFlagAndWorkCenterIdAndStatus(
                    DEL_FLAG_NORMAL, center.getId(), DispatchTaskService.STATUS_PROCESSING);
            if (!tasks.isEmpty()) {
                fillCurrentTask(vo, tasks.get(0));
            }
            result.add(vo);
        }
        return result;
    }

    private DashboardSummaryVO buildSummary() {
        DashboardSummaryVO vo = new DashboardSummaryVO();

        DashboardSummaryVO.OrderOverview orderOverview = new DashboardSummaryVO.OrderOverview();
        orderOverview.setInProgress(workOrderRepository.countByDelFlagAndStatus(DEL_FLAG_NORMAL, WorkOrderService.STATUS_IN_PROGRESS));
        orderOverview.setPending(workOrderRepository.countByDelFlagAndStatus(DEL_FLAG_NORMAL, WorkOrderService.STATUS_PLANNED)
                + workOrderRepository.countByDelFlagAndStatus(DEL_FLAG_NORMAL, WorkOrderService.STATUS_RELEASED));
        orderOverview.setCompleted(workOrderRepository.countByDelFlagAndStatus(DEL_FLAG_NORMAL, WorkOrderService.STATUS_COMPLETED));
        vo.setOrderOverview(orderOverview);

        // 工位状态统计：base 服务不可用时置 0（看板不阻断，仅告警）
        DashboardSummaryVO.WorkCenterOverview centerOverview = new DashboardSummaryVO.WorkCenterOverview();
        try {
            List<WorkCenterDTO> centers = workCenterClient.listWorkCenters(null);
            centerOverview.setIdle(centers.stream().filter(c -> "IDLE".equals(c.getStatus())).count());
            centerOverview.setBusy(centers.stream().filter(c -> "BUSY".equals(c.getStatus())).count());
            centerOverview.setOffline(centers.stream().filter(c -> "OFFLINE".equals(c.getStatus())).count());
        } catch (Exception e) {
            log.warn("看板聚合工位状态失败: {}", e.getMessage());
        }
        vo.setWorkCenterOverview(centerOverview);

        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        LocalDateTime dayEnd = LocalDate.now().atTime(LocalTime.MAX);
        DashboardSummaryVO.TodayOutput output = new DashboardSummaryVO.TodayOutput();
        output.setCompleted(dispatchTaskRepository.sumCompletedQtyBetween(dayStart, dayEnd));
        output.setPlanned(dispatchTaskRepository.sumDispatchedQtyBetween(dayStart, dayEnd));
        output.setPercentage(output.getPlanned() == 0 ? 0
                : (int) Math.min(100, output.getCompleted() * 100 / output.getPlanned()));
        vo.setTodayOutput(output);
        return vo;
    }

    /** 填充工位当前任务信息（工单号/工序/操作员均为聚合字段，失败仅缺失不阻断） */
    private void fillCurrentTask(WorkCenterLoadVO vo, DispatchTask task) {
        vo.setTaskNo(task.getTaskNo());
        workOrderRepository.findById(task.getWorkOrderId())
                .ifPresent(order -> vo.setOrderNo(order.getOrderNo()));
        try {
            vo.setStepName(workflowStepClient.getStep(task.getWorkflowStepId()).getStepName());
        } catch (Exception e) {
            log.warn("看板聚合工序信息失败（任务 {}）: {}", task.getTaskNo(), e.getMessage());
        }
        try {
            vo.setOperatorName(userClient.getUser(task.getOperatorId()).getRealName());
        } catch (Exception e) {
            log.warn("看板聚合操作员信息失败（任务 {}）: {}", task.getTaskNo(), e.getMessage());
        }
    }
}
