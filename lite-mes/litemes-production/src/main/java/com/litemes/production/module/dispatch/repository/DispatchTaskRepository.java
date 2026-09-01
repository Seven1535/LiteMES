package com.litemes.production.module.dispatch.repository;

import com.litemes.production.module.dispatch.entity.DispatchTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 派工任务仓库：所有查询默认过滤 delFlag="0"（逻辑删除）。
 */
public interface DispatchTaskRepository extends JpaRepository<DispatchTask, String> {

    Page<DispatchTask> findByDelFlag(String delFlag, Pageable pageable);

    Page<DispatchTask> findByDelFlagAndWorkOrderId(String delFlag, String workOrderId, Pageable pageable);

    Page<DispatchTask> findByDelFlagAndWorkOrderIdAndStatus(String delFlag, String workOrderId, String status, Pageable pageable);

    Page<DispatchTask> findByDelFlagAndWorkCenterId(String delFlag, String workCenterId, Pageable pageable);

    Page<DispatchTask> findByDelFlagAndWorkCenterIdAndStatus(String delFlag, String workCenterId, String status, Pageable pageable);

    Page<DispatchTask> findByDelFlagAndOperatorId(String delFlag, String operatorId, Pageable pageable);

    Page<DispatchTask> findByDelFlagAndOperatorIdAndStatus(String delFlag, String operatorId, String status, Pageable pageable);

    /** 工单下全部任务（工单关闭级联、派工累计校验用） */
    List<DispatchTask> findByDelFlagAndWorkOrderId(String delFlag, String workOrderId);

    /** 工单下指定状态集合的任务（工单关闭时级联关闭未完成任务） */
    List<DispatchTask> findByDelFlagAndWorkOrderIdAndStatusIn(String delFlag, String workOrderId, Collection<String> statuses);

    /** 某工序已派工总量（排除级联关闭的任务）：派工数量上限校验用（业务规则 8.3） */
    @Query("SELECT COALESCE(SUM(t.quantity), 0) FROM DispatchTask t " +
            "WHERE t.workOrderId = :workOrderId AND t.workflowStepId = :workflowStepId " +
            "AND t.status <> 'CLOSED' AND t.delFlag = '0'")
    long sumDispatchQty(@Param("workOrderId") String workOrderId,
                        @Param("workflowStepId") String workflowStepId);

    /** 工位进行中任务数（工位删除保护 8.4 / 报工后工位回 IDLE 判断） */
    long countByDelFlagAndWorkCenterIdAndStatus(String delFlag, String workCenterId, String status);

    /** 看板：工位的进行中任务列表（工位负载看板取当前任务） */
    List<DispatchTask> findByDelFlagAndWorkCenterIdAndStatus(String delFlag, String workCenterId, String status);

    /** 看板：今日完成任务的派工数量合计（今日产出-完成） */
    @Query("SELECT COALESCE(SUM(t.quantity), 0) FROM DispatchTask t " +
            "WHERE t.status = 'COMPLETED' AND t.completedAt BETWEEN :start AND :end AND t.delFlag = '0'")
    long sumCompletedQtyBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /** 看板：今日派出的任务数量合计（今日产出-计划） */
    @Query("SELECT COALESCE(SUM(t.quantity), 0) FROM DispatchTask t " +
            "WHERE t.dispatchedAt BETWEEN :start AND :end AND t.delFlag = '0'")
    long sumDispatchedQtyBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
