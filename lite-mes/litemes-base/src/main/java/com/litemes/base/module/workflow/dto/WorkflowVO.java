package com.litemes.base.module.workflow.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 工艺路线版本展示对象（列表用） */
@Data
public class WorkflowVO {

    private String id;
    private String productId;
    /** 冗余产品编码/名称，避免前端逐行再查产品 */
    private String productCode;
    private String productName;
    private Integer version;
    private String versionName;
    private String description;
    private Boolean isActive;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
