package com.litemes.base.module.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 新增产品请求（状态默认 ACTIVE） */
@Data
public class ProductCreateRequest {

    @NotBlank(message = "产品编码不能为空")
    @Size(max = 32, message = "产品编码长度不能超过 32 位")
    private String productCode;

    @NotBlank(message = "产品名称不能为空")
    @Size(max = 128, message = "产品名称长度不能超过 128 位")
    private String productName;

    @Size(max = 255, message = "产品描述长度不能超过 255 位")
    private String description;

    @Size(max = 255, message = "图纸地址长度不能超过 255 位")
    private String drawingUrl;

    @Pattern(regexp = "ACTIVE|INACTIVE", message = "状态只能是 ACTIVE 或 INACTIVE")
    private String status;
}
