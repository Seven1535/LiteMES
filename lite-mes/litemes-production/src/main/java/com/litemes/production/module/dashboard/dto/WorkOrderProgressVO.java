package com.litemes.production.module.dashboard.dto;

import lombok.Data;

/** 在制工单进度（看板进度条列表） */
@Data
public class WorkOrderProgressVO {

    private String id;
    private String orderNo;
    /** 冗余：产品名称（Feign 聚合） */
    private String productName;
    private Integer quantity;
    private Integer completedQty;
    /** 进度百分比（0-100） */
    private int percentage;
    private Integer priority;
    private String status;
}
