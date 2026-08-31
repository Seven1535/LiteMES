package com.litemes.base.module.workflow.entity;

import com.litemes.base.config.DbSchema;
import com.litemes.common.core.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 工序步骤（对应《设计规格说明书》5.2 WORKFLOW_STEP 表）。
 * 一个工艺版本包含多个有序工序；posX/posY 为 Vue Flow 画布坐标。
 */
@Getter
@Setter
@Entity
@Table(name = "WORKFLOW_STEP", schema = DbSchema.NAME)
public class WorkflowStep extends BaseEntity {

    /** 所属工艺路线 ID */
    @Column(name = "WORKFLOW_ID", length = 36, nullable = false)
    private String workflowId;

    /** 工序编码（版本内唯一，业务层保证） */
    @Column(name = "STEP_CODE", length = 32, nullable = false)
    private String stepCode;

    /** 工序名称（如"粗车"、"精磨"、"检验"） */
    @Column(name = "STEP_NAME", length = 128, nullable = false)
    private String stepName;

    /** 工序顺序号 */
    @Column(name = "STEP_ORDER", nullable = false)
    private Integer stepOrder;

    /** 工序描述 */
    @Column(name = "DESCRIPTION", length = 255)
    private String description;

    /** 要求工位类型（可选，用于派工推荐） */
    @Column(name = "REQUIRED_WORK_CENTER_TYPE", length = 32)
    private String requiredWorkCenterType;

    /** 预估工时（分钟） */
    @Column(name = "ESTIMATED_MINUTES")
    private Integer estimatedMinutes;

    /** 流程图 X 坐标（画布定位用） */
    @Column(name = "POS_X", nullable = false)
    private Double posX = 0.0;

    /** 流程图 Y 坐标 */
    @Column(name = "POS_Y", nullable = false)
    private Double posY = 0.0;
}
