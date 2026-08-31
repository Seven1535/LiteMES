package com.litemes.base.module.workcenter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 修改工位请求（编码不可改，保证派工任务引用的可读性） */
@Data
public class WorkCenterUpdateRequest {

    @NotBlank(message = "工位名称不能为空")
    @Size(max = 128, message = "工位名称长度不能超过 128 位")
    private String centerName;

    @Size(max = 32, message = "工位类型长度不能超过 32 位")
    private String centerType;
}
