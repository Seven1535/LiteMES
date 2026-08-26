package com.litemes.api.base.dto;

import lombok.Data;

/** 工位契约 DTO（字段与《设计规格说明书》5.2 WORK_CENTER 表对齐） */
@Data
public class WorkCenterDTO {

    private String id;
    private String centerCode;
    private String centerName;
    private String centerType;
    private String status;
}
