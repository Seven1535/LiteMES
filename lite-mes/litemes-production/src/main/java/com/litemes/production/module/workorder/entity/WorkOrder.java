package com.litemes.production.module.workorder.entity;

import com.litemes.common.core.BaseEntity;
import com.litemes.production.config.DbSchema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 生产工单（对应《设计规格说明书》5.2 WORK_ORDER 表）。
 * 状态机：PLANNED → RELEASED → IN_PROGRESS → COMPLETED；RELEASED/IN_PROGRESS/COMPLETED 可关闭（CLOSED）。
 * 创建时锁定产品当前生效工艺版本（workflowId），编号由 Redis INCR 生成（CodeGenerator）。
 */
@Getter
@Setter
@Entity
@Table(name = "WORK_ORDER", schema = DbSchema.NAME)
public class WorkOrder extends BaseEntity {

    /** 工单编号（自动生成：WO-yyyyMMdd-NNN，不可改） */
    @Column(name = "ORDER_NO", length = 32, nullable = false, unique = true)
    private String orderNo;

    /** 产品 ID（跨服务引用，不加物理外键） */
    @Column(name = "PRODUCT_ID", length = 36, nullable = false)
    private String productId;

    /** 锁定的工艺路线版本 ID（跨服务引用） */
    @Column(name = "WORKFLOW_ID", length = 36, nullable = false)
    private String workflowId;

    /** 计划数量 */
    @Column(name = "QUANTITY", nullable = false)
    private Integer quantity;

    /** 已完工数量（报工累计，默认 0） */
    @Column(name = "COMPLETED_QTY", nullable = false)
    private Integer completedQty = 0;

    /** 优先级（1=紧急, 2=高, 3=中, 4=低） */
    @Column(name = "PRIORITY", nullable = false)
    private Integer priority;

    /** 状态：PLANNED / RELEASED / IN_PROGRESS / COMPLETED / CLOSED */
    @Column(name = "STATUS", length = 16, nullable = false)
    private String status;

    /** 计划开始日期 */
    @Column(name = "PLAN_START_DATE")
    private LocalDate planStartDate;

    /** 计划完成日期 */
    @Column(name = "PLAN_END_DATE")
    private LocalDate planEndDate;

    /** 实际开始日期（首个任务开工时写入） */
    @Column(name = "ACTUAL_START_DATE")
    private LocalDate actualStartDate;

    /** 实际完成日期 */
    @Column(name = "ACTUAL_END_DATE")
    private LocalDate actualEndDate;

    /** 备注 */
    @Column(name = "REMARK", length = 255)
    private String remark;
}
