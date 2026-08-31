package com.litemes.base.module.workcenter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 新增工位请求（状态默认 IDLE） */
@Data
public class WorkCenterCreateRequest {

    @NotBlank(message = "工位编码不能为空")
    @Size(max = 32, message = "工位编码长度不能超过 32 位")
    private String centerCode;

    @NotBlank(message = "工位名称不能为空")
    @Size(max = 128, message = "工位名称长度不能超过 128 位")
    private String centerName;

    @Size(max = 32, message = "工位类型长度不能超过 32 位")
    private String centerType;
}
