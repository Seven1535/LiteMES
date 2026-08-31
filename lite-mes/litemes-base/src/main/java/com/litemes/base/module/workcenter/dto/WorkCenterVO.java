package com.litemes.base.module.workcenter.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 工位展示对象（对外接口统一返回本对象） */
@Data
public class WorkCenterVO {

    private String id;
    private String centerCode;
    private String centerName;
    private String centerType;
    private String operatorId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
