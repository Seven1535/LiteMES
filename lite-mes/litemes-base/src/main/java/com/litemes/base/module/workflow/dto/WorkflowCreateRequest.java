package com.litemes.base.module.workflow.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 新建工艺路线版本请求（版本号取产品当前最大版本 + 1） */
@Data
public class WorkflowCreateRequest {

    @Size(max = 32, message = "版本标识长度不能超过 32 位")
    private String versionName;

    @Size(max = 255, message = "版本说明长度不能超过 255 位")
    private String description;

    /** 复制来源版本（可选：新版本从其复制工序和连线） */
    @Min(value = 1, message = "复制来源版本号必须大于 0")
    private Integer copyFromVersion;
}
