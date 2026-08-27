package com.litemes.base.module.product.entity;

import com.litemes.base.config.DbSchema;
import com.litemes.common.core.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 产品（对应《设计规格说明书》5.2 PRODUCT 表）。
 * 状态：ACTIVE=启用（可被工单选择），INACTIVE=停用（存量工单不受影响）。
 */
@Getter
@Setter
@Entity
@Table(name = "PRODUCT", schema = DbSchema.NAME)
public class Product extends BaseEntity {

    /** 产品编码（唯一，业务层保证） */
    @Column(name = "PRODUCT_CODE", length = 32, nullable = false)
    private String productCode;

    /** 产品名称 */
    @Column(name = "PRODUCT_NAME", length = 128, nullable = false)
    private String productName;

    /** 产品描述 */
    @Column(name = "DESCRIPTION", length = 255)
    private String description;

    /** 图纸文件地址（可选，文件服务未接入前仅存地址字符串） */
    @Column(name = "DRAWING_URL", length = 255)
    private String drawingUrl;

    /** 状态：ACTIVE / INACTIVE */
    @Column(name = "STATUS", length = 16, nullable = false)
    private String status;
}
