package com.litemes.production.module.dashboard.dto;

import lombok.Data;

/** 工位负载（看板：每个工位当前在干什么） */
@Data
public class WorkCenterLoadVO {

    private String id;
    private String centerCode;
    private String centerName;
    private String status;

    /** 当前进行中任务信息（无任务时为"空闲"，以下字段为 null） */
    private String taskNo;
    private String orderNo;
    private String stepName;
    private String operatorName;
}
