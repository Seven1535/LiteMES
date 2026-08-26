package com.litemes.api.base.dto;

import lombok.Data;

/** 产品契约 DTO（字段与《设计规格说明书》5.2 PRODUCT 表对齐） */
@Data
public class ProductDTO {

    private String id;
    private String productCode;
    private String productName;
    private String description;
    private String status;
}
