package com.litemes.production.module.workorder.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 工单展示对象（产品/工艺名称经 Feign 聚合） */
@Data
public class WorkOrderVO {

    private String id;
    private String orderNo;
    private String productId;
    /** 冗余：产品编码/名称（Feign 聚合，避免前端逐行再查） */
    private String productCode;
    private String productName;
    private String workflowId;
    /** 冗余：锁定的工艺版本标识 */
    private String workflowVersionName;
    private Integer quantity;
    private Integer completedQty;
    private Integer priority;
    private String status;
    private LocalDate planStartDate;
    private LocalDate planEndDate;
    private LocalDate actualStartDate;
    private LocalDate actualEndDate;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
