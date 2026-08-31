package com.litemes.base.module.workcenter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** 更新工位状态请求（派工开始/结束、手工停用） */
@Data
public class WorkCenterStatusRequest {

    @NotBlank(message = "状态不能为空")
    @Pattern(regexp = "IDLE|BUSY|OFFLINE", message = "状态只能是 IDLE、BUSY 或 OFFLINE")
    private String status;
}
