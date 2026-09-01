package com.litemes.production.module.dispatch.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 派工任务展示对象（工单编号/工序/工位/操作员名称经 Feign 聚合） */
@Data
public class DispatchTaskVO {

    private String id;
    private String taskNo;
    private String workOrderId;
    /** 冗余：工单编号（聚合自本服务工单表） */
    private String workOrderNo;
    private String workflowStepId;
    /** 冗余：工序名称（Feign 聚合） */
    private String stepName;
    private String workCenterId;
    /** 冗余：工位名称（Feign 聚合） */
    private String workCenterName;
    private String operatorId;
    /** 冗余：操作员姓名（Feign 聚合） */
    private String operatorName;
    private Integer quantity;
    private Integer completedQty;
    private String status;
    private LocalDateTime dispatchedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
