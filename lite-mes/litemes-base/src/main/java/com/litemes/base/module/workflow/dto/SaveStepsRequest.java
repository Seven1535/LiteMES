package com.litemes.base.module.workflow.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 批量保存工序请求（画布保存，全量覆盖）。
 * id 为 null 表示新增；提交列表中缺失的已有工序将被删除（其连线一并清理）。
 */
@Data
public class SaveStepsRequest {

    @NotEmpty(message = "工序列表不能为空")
    @Valid
    private List<StepItem> steps;

    @Data
    public static class StepItem {

        /** 已有工序 ID（新增时为空） */
        private String id;

        @NotBlank(message = "工序编码不能为空")
        @Size(max = 32, message = "工序编码长度不能超过 32 位")
        private String stepCode;

        @NotBlank(message = "工序名称不能为空")
        @Size(max = 128, message = "工序名称长度不能超过 128 位")
        private String stepName;

        private Integer stepOrder;

        @Size(max = 255, message = "工序描述长度不能超过 255 位")
        private String description;

        @Size(max = 32, message = "工位类型长度不能超过 32 位")
        private String requiredWorkCenterType;

        private Integer estimatedMinutes;

        private Double posX;
        private Double posY;
    }
}
