package com.litemes.production.module.workorder.service;

import com.litemes.api.base.ProductClient;
import com.litemes.api.base.WorkflowClient;
import com.litemes.api.base.dto.ProductDTO;
import com.litemes.api.base.dto.WorkflowDTO;
import com.litemes.common.code.CodeGenerator;
import com.litemes.common.core.BusinessException;
import com.litemes.common.core.PageResult;
import com.litemes.production.module.workorder.dto.WorkOrderCreateRequest;
import com.litemes.production.module.workorder.dto.WorkOrderQueryRequest;
import com.litemes.production.module.workorder.dto.WorkOrderUpdateRequest;
import com.litemes.production.module.workorder.dto.WorkOrderVO;
import com.litemes.production.module.workorder.entity.WorkOrder;
import com.litemes.production.module.workorder.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 工单业务：分页查询（Feign 聚合产品/工艺名称）/ 创建（锁定生效工艺 + Redis 编号）/ 下达 / 关闭 / 删除。
 * 状态机见《设计规格说明书》4.2：
 * PLANNED →（下达）→ RELEASED →（全部派工）→ IN_PROGRESS →（全部报工）→ COMPLETED；
 * RELEASED / IN_PROGRESS / COMPLETED 均可关闭（CLOSED）。
 * 业务规则（8.2）：仅 PLANNED 可下达/编辑/删除；已下达不可删除只能关闭；CLOSED 终态不可操作。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkOrderService {

    private static final String DEL_FLAG_NORMAL = "0";
    private static final String DEL_FLAG_DELETED = "1";

    public static final String STATUS_PLANNED = "PLANNED";
    public static final String STATUS_RELEASED = "RELEASED";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CLOSED = "CLOSED";

    private final WorkOrderRepository workOrderRepository;
    private final CodeGenerator codeGenerator;
    private final ProductClient productClient;
    private final WorkflowClient workflowClient;

    /** 分页查询：编号模糊 + 状态精确；产品/工艺名称经 Feign 聚合（按产品去重批量查） */
    public PageResult<WorkOrderVO> page(WorkOrderQueryRequest query) {
        Pageable pageable = PageRequest.of(query.getPageNum() - 1, query.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        String orderNo = trimToNull(query.getOrderNo());
        String status = trimToNull(query.getStatus());

        Page<WorkOrder> page;
        if (orderNo != null && status != null) {
            page = workOrderRepository.findByDelFlagAndOrderNoContainingAndStatus(DEL_FLAG_NORMAL, orderNo, status, pageable);
        } else if (orderNo != null) {
            page = workOrderRepository.findByDelFlagAndOrderNoContaining(DEL_FLAG_NORMAL, orderNo, pageable);
        } else if (status != null) {
            page = workOrderRepository.findByDelFlagAndStatus(DEL_FLAG_NORMAL, status, pageable);
        } else {
            page = workOrderRepository.findByDelFlag(DEL_FLAG_NORMAL, pageable);
        }

        Map<String, ProductDTO> productCache = new HashMap<>();
        Map<String, WorkflowDTO> workflowCache = new HashMap<>();
        return PageResult.of(page.getContent().stream()
                        .map(order -> toVO(order, productCache, workflowCache)).toList(),
                page.getTotalElements(), query.getPageNum(), query.getPageSize());
    }

    public WorkOrderVO getById(String id) {
        return toVO(loadActive(id), new HashMap<>(), new HashMap<>());
    }

    /** 创建工单：校验产品可用 → 锁定当前生效工艺版本 → Redis INCR 生成编号 */
    @Transactional
    public WorkOrderVO create(WorkOrderCreateRequest request) {
        checkPlanDates(request.getPlanStartDate() == null ? null : request.getPlanStartDate(),
                request.getPlanEndDate());
        ProductDTO product = productClient.getProduct(request.getProductId());
        if (!"ACTIVE".equals(product.getStatus())) {
            throw new BusinessException(400, "产品已停用，不能创建工单");
        }
        WorkflowDTO workflow = workflowClient.getActiveWorkflow(request.getProductId());

        WorkOrder order = new WorkOrder();
        order.setId(UUID.randomUUID().toString());
        order.setOrderNo(codeGenerator.generateWorkOrderNo());
        order.setProductId(product.getId());
        order.setWorkflowId(workflow.getId());
        order.setQuantity(request.getQuantity());
        order.setCompletedQty(0);
        order.setPriority(request.getPriority());
        order.setStatus(STATUS_PLANNED);
        order.setPlanStartDate(request.getPlanStartDate());
        order.setPlanEndDate(request.getPlanEndDate());
        order.setRemark(trimToNull(request.getRemark()));
        return toVO(workOrderRepository.save(order), Map.of(product.getId(), product), Map.of(workflow.getId(), workflow));
    }

    /** 更新工单：仅已计划状态可改（8.2） */
    @Transactional
    public WorkOrderVO update(String id, WorkOrderUpdateRequest request) {
        WorkOrder order = loadActive(id);
        requireStatus(order, STATUS_PLANNED, "仅已计划状态的工单可编辑");
        checkPlanDates(request.getPlanStartDate(), request.getPlanEndDate());
        order.setQuantity(request.getQuantity());
        order.setPriority(request.getPriority());
        order.setPlanStartDate(request.getPlanStartDate());
        order.setPlanEndDate(request.getPlanEndDate());
        order.setRemark(trimToNull(request.getRemark()));
        return toVO(workOrderRepository.save(order), new HashMap<>(), new HashMap<>());
    }

    /** 下达工单：仅已计划可下达（8.2）；实际开始日期等首个任务开工时写入 */
    @Transactional
    public WorkOrderVO release(String id) {
        WorkOrder order = loadActive(id);
        requireStatus(order, STATUS_PLANNED, "仅已计划状态的工单可下达");
        order.setStatus(STATUS_RELEASED);
        return toVO(workOrderRepository.save(order), new HashMap<>(), new HashMap<>());
    }

    /** 关闭工单：已下达/生产中/已完工均可关闭；CLOSED 为终态（8.2） */
    @Transactional
    public WorkOrderVO close(String id) {
        WorkOrder order = loadActive(id);
        if (STATUS_CLOSED.equals(order.getStatus())) {
            throw new BusinessException(400, "工单已关闭");
        }
        if (STATUS_PLANNED.equals(order.getStatus())) {
            throw new BusinessException(400, "已计划的工单请直接删除，无需关闭");
        }
        order.setStatus(STATUS_CLOSED);
        return toVO(workOrderRepository.save(order), new HashMap<>(), new HashMap<>());
    }

    /** 删除工单：逻辑删除，仅已计划可删（已下达的只能关闭，8.2） */
    @Transactional
    public void delete(String id) {
        WorkOrder order = loadActive(id);
        requireStatus(order, STATUS_PLANNED, "仅已计划状态的工单可删除，已下达的工单请关闭");
        order.setDelFlag(DEL_FLAG_DELETED);
        workOrderRepository.save(order);
    }

    private WorkOrder loadActive(String id) {
        return workOrderRepository.findById(id)
                .filter(o -> DEL_FLAG_NORMAL.equals(o.getDelFlag()))
                .orElseThrow(() -> new BusinessException(404, "工单不存在"));
    }

    private void requireStatus(WorkOrder order, String expected, String message) {
        if (!expected.equals(order.getStatus())) {
            throw new BusinessException(400, message);
        }
    }

    private void checkPlanDates(java.time.LocalDate start, java.time.LocalDate end) {
        if (start != null && end != null && end.isBefore(start)) {
            throw new BusinessException(400, "计划完成日期不能早于计划开始日期");
        }
    }

    private WorkOrderVO toVO(WorkOrder order, Map<String, ProductDTO> productCache, Map<String, WorkflowDTO> workflowCache) {
        WorkOrderVO vo = new WorkOrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setProductId(order.getProductId());
        vo.setWorkflowId(order.getWorkflowId());
        vo.setQuantity(order.getQuantity());
        vo.setCompletedQty(order.getCompletedQty());
        vo.setPriority(order.getPriority());
        vo.setStatus(order.getStatus());
        vo.setPlanStartDate(order.getPlanStartDate());
        vo.setPlanEndDate(order.getPlanEndDate());
        vo.setActualStartDate(order.getActualStartDate());
        vo.setActualEndDate(order.getActualEndDate());
        vo.setRemark(order.getRemark());
        vo.setCreatedAt(order.getCreatedAt());
        vo.setUpdatedAt(order.getUpdatedAt());

        // Feign 聚合：产品/工艺查询失败不阻断列表展示（仅冗余字段缺失）
        try {
            ProductDTO product = productCache.computeIfAbsent(order.getProductId(), productClient::getProduct);
            vo.setProductCode(product.getProductCode());
            vo.setProductName(product.getProductName());
        } catch (Exception e) {
            log.warn("聚合产品信息失败（工单 {}）: {}", order.getOrderNo(), e.getMessage());
        }
        try {
            WorkflowDTO workflow = workflowCache.computeIfAbsent(order.getWorkflowId(), workflowClient::getWorkflow);
            vo.setWorkflowVersionName(workflow.getVersionName());
        } catch (Exception e) {
            log.warn("聚合工艺信息失败（工单 {}）: {}", order.getOrderNo(), e.getMessage());
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
