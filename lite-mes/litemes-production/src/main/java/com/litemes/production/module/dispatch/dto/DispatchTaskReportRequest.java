package com.litemes.production.module.dispatch.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 报工请求（业务规则 8.3：报工数量不超过任务剩余未完成数量） */
@Data
public class DispatchTaskReportRequest {

    /** 本次报工数量 */
    @NotNull(message = "报工数量不能为空")
    @Min(value = 1, message = "报工数量至少为 1")
    private Integer quantity;
}
