package com.litemes.api.base.dto;

import lombok.Data;

/** 工艺路线契约 DTO（字段与《设计规格说明书》5.2 WORKFLOW 表对齐） */
@Data
public class WorkflowDTO {

    private String id;
    private String productId;
    private Integer version;
    private String versionName;
    private Boolean isActive;
    private String status;
}
