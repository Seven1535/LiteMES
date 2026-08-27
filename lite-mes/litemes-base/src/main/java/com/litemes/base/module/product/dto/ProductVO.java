package com.litemes.base.module.product.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 产品展示对象（对外接口统一返回本对象） */
@Data
public class ProductVO {

    private String id;
    private String productCode;
    private String productName;
    private String description;
    private String drawingUrl;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
