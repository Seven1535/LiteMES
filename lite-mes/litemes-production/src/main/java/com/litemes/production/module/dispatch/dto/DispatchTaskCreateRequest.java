package com.litemes.production.module.dispatch.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 派工请求（业务规则 8.3：必须选工序/工位/操作员，数量不超过工单剩余可派量） */
@Data
public class DispatchTaskCreateRequest {

    /** 所属工单 ID */
    @NotBlank(message = "工单不能为空")
    private String workOrderId;

    /** 工艺工序 ID（必须属于工单锁定的工艺版本） */
    @NotBlank(message = "工序不能为空")
    private String workflowStepId;

    /** 执行工位 ID */
    @NotBlank(message = "工位不能为空")
    private String workCenterId;

    /** 操作员 ID */
    @NotBlank(message = "操作员不能为空")
    private String operatorId;

    /** 派工数量 */
    @NotNull(message = "派工数量不能为空")
    @Min(value = 1, message = "派工数量至少为 1")
    private Integer quantity;

    /** 备注（可选） */
    private String remark;
}
