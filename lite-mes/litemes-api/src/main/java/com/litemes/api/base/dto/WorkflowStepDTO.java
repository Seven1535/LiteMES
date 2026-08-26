package com.litemes.api.base.dto;

import lombok.Data;

/** 工序步骤契约 DTO（字段与《设计规格说明书》5.2 WORKFLOW_STEP 表对齐） */
@Data
public class WorkflowStepDTO {

    private String id;
    private String workflowId;
    private String stepCode;
    private String stepName;
    private Integer stepOrder;
    private String requiredWorkCenterType;
    private Integer estimatedMinutes;
}
