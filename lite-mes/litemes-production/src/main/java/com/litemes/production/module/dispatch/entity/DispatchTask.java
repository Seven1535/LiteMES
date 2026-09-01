package com.litemes.production.module.dispatch.entity;

import com.litemes.common.core.BaseEntity;
import com.litemes.production.config.DbSchema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 派工任务（对应《设计规格说明书》5.2 DISPATCH_TASK 表）。
 * 状态机（4.3）：PENDING →（开工）→ PROCESSING →（报工累计满）→ COMPLETED；工单关闭时未完成任务级联 CLOSED。
 * 同一工序可拆分派到多个工位（业务规则 8.3），编号由 Redis INCR 生成（CodeGenerator）。
 */
@Getter
@Setter
@Entity
@Table(name = "DISPATCH_TASK", schema = DbSchema.NAME)
public class DispatchTask extends BaseEntity {

    /** 任务编号（自动生成：TK-yyyyMMdd-NNNN，不可改） */
    @Column(name = "TASK_NO", length = 32, nullable = false, unique = true)
    private String taskNo;

    /** 所属工单 ID */
    @Column(name = "WORK_ORDER_ID", length = 36, nullable = false)
    private String workOrderId;

    /** 工艺工序 ID（跨服务引用，不加物理外键） */
    @Column(name = "WORKFLOW_STEP_ID", length = 36, nullable = false)
    private String workflowStepId;

    /** 执行工位 ID（跨服务引用） */
    @Column(name = "WORK_CENTER_ID", length = 36, nullable = false)
    private String workCenterId;

    /** 操作员 ID（跨服务引用） */
    @Column(name = "OPERATOR_ID", length = 36, nullable = false)
    private String operatorId;

    /** 派工数量 */
    @Column(name = "QUANTITY", nullable = false)
    private Integer quantity;

    /** 已报工数量（默认 0，累计达到派工数量任务自动完成） */
    @Column(name = "COMPLETED_QTY", nullable = false)
    private Integer completedQty = 0;

    /** 状态：PENDING / PROCESSING / COMPLETED / CLOSED */
    @Column(name = "STATUS", length = 16, nullable = false)
    private String status;

    /** 派工时间（创建即写入） */
    @Column(name = "DISPATCHED_AT")
    private LocalDateTime dispatchedAt;

    /** 开工时间（开始任务时写入） */
    @Column(name = "STARTED_AT")
    private LocalDateTime startedAt;

    /** 完工时间（报工累计满时写入） */
    @Column(name = "COMPLETED_AT")
    private LocalDateTime completedAt;

    /** 备注 */
    @Column(name = "REMARK", length = 255)
    private String remark;
}
