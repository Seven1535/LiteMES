package com.litemes.base.module.workflow.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** 批量保存连线请求（画布连线全量覆盖，可为空列表表示清空连线） */
@Data
public class SaveLinksRequest {

    @NotNull(message = "连线列表不能为 null")
    @Valid
    private List<LinkItem> links;

    @Data
    public static class LinkItem {

        @NotBlank(message = "起点工序不能为空")
        private String sourceStepId;

        @NotBlank(message = "终点工序不能为空")
        private String targetStepId;
    }
}
