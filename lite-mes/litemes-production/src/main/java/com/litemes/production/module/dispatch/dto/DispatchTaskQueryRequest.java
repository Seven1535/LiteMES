package com.litemes.production.module.dispatch.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/** 派工任务分页查询参数 */
@Data
public class DispatchTaskQueryRequest {

    @Min(value = 1, message = "页码从 1 开始")
    private int pageNum = 1;

    @Min(value = 1, message = "每页条数至少 1")
    @Max(value = 100, message = "每页条数最多 100")
    private int pageSize = 10;

    /** 工单 ID（精确匹配，可选） */
    private String workOrderId;

    /** 工位 ID（精确匹配，可选） */
    private String workCenterId;

    /** 操作员 ID（精确匹配，可选，用于"我的任务"） */
    private String operatorId;

    /** 状态（精确匹配，可选）：PENDING / PROCESSING / COMPLETED / CLOSED */
    private String status;
}
