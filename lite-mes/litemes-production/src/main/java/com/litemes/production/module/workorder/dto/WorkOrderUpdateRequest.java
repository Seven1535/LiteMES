package com.litemes.production.module.workorder.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/** 更新工单请求（仅已计划状态可改；产品/工艺/编号不可改） */
@Data
public class WorkOrderUpdateRequest {

    @NotNull(message = "计划数量不能为空")
    @Min(value = 1, message = "计划数量至少 1")
    private Integer quantity;

    @NotNull(message = "优先级不能为空")
    @Min(value = 1, message = "优先级范围 1-4")
    @Max(value = 4, message = "优先级范围 1-4")
    private Integer priority;

    private LocalDate planStartDate;

    private LocalDate planEndDate;

    @Size(max = 255, message = "备注长度不能超过 255 位")
    private String remark;
}
