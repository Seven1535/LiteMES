package com.litemes.base.module.workflow.dto;

import lombok.Data;

import java.util.List;

/** 工艺路线详情（含工序步骤和连线，编辑器渲染用） */
@Data
public class WorkflowDetailVO {

    private WorkflowVO workflow;
    private List<StepVO> steps;
    private List<LinkVO> links;

    @Data
    public static class StepVO {
        private String id;
        private String stepCode;
        private String stepName;
        private Integer stepOrder;
        private String description;
        private String requiredWorkCenterType;
        private Integer estimatedMinutes;
        private Double posX;
        private Double posY;
    }

    @Data
    public static class LinkVO {
        private String id;
        private String sourceStepId;
        private String targetStepId;
    }
}
