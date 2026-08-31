package com.litemes.base.module.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 修改工艺路线元数据请求（仅草稿可改） */
@Data
public class WorkflowUpdateRequest {

    @NotBlank(message = "版本标识不能为空")
    @Size(max = 32, message = "版本标识长度不能超过 32 位")
    private String versionName;

    @Size(max = 255, message = "版本说明长度不能超过 255 位")
    private String description;
}
