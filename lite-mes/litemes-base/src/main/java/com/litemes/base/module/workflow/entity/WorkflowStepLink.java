package com.litemes.base.module.workflow.entity;

import com.litemes.base.config.DbSchema;
import com.litemes.common.core.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 工序连线（对应架构设计 5.2 workflow_step_link 表，前驱后继关系）。
 * 与 Vue Flow 的 edge 一一对应：sourceStepId → targetStepId。
 */
@Getter
@Setter
@Entity
@Table(name = "WORKFLOW_STEP_LINK", schema = DbSchema.NAME)
public class WorkflowStepLink extends BaseEntity {

    /** 所属工艺路线 ID（冗余，便于按版本整体读写） */
    @Column(name = "WORKFLOW_ID", length = 36, nullable = false)
    private String workflowId;

    /** 起点工序 ID */
    @Column(name = "SOURCE_STEP_ID", length = 36, nullable = false)
    private String sourceStepId;

    /** 终点工序 ID */
    @Column(name = "TARGET_STEP_ID", length = 36, nullable = false)
    private String targetStepId;
}
