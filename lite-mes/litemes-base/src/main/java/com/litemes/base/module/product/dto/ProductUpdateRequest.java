package com.litemes.base.module.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 修改产品请求（编码不可改，保证工单等下游引用的可读性） */
@Data
public class ProductUpdateRequest {

    @NotBlank(message = "产品名称不能为空")
    @Size(max = 128, message = "产品名称长度不能超过 128 位")
    private String productName;

    @Size(max = 255, message = "产品描述长度不能超过 255 位")
    private String description;

    @Size(max = 255, message = "图纸地址长度不能超过 255 位")
    private String drawingUrl;

    @NotBlank(message = "状态不能为空")
    @Pattern(regexp = "ACTIVE|INACTIVE", message = "状态只能是 ACTIVE 或 INACTIVE")
    private String status;
}
