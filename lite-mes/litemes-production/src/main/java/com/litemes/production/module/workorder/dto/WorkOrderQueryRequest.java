package com.litemes.production.module.workorder.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/** 工单分页查询参数 */
@Data
public class WorkOrderQueryRequest {

    @Min(value = 1, message = "页码从 1 开始")
    private int pageNum = 1;

    @Min(value = 1, message = "每页条数至少 1")
    @Max(value = 100, message = "每页条数最多 100")
    private int pageSize = 10;

    /** 工单编号（模糊匹配，可选） */
    private String orderNo;

    /** 状态（精确匹配，可选）：PLANNED / RELEASED / IN_PROGRESS / COMPLETED / CLOSED */
    private String status;
}
