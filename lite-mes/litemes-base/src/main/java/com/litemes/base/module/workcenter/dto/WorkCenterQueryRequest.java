package com.litemes.base.module.workcenter.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/** 工位分页查询参数 */
@Data
public class WorkCenterQueryRequest {

    @Min(value = 1, message = "页码从 1 开始")
    private int pageNum = 1;

    @Min(value = 1, message = "每页条数至少 1")
    @Max(value = 100, message = "每页条数最多 100")
    private int pageSize = 10;

    /** 工位编码（模糊匹配，可选） */
    private String centerCode;

    /** 工位名称（模糊匹配，可选） */
    private String centerName;

    /** 状态（精确匹配，可选）：IDLE / BUSY / OFFLINE */
    private String status;
}
