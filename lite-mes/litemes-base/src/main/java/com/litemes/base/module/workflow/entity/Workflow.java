package com.litemes.base.module.workflow.entity;

import com.litemes.base.config.DbSchema;
import com.litemes.common.core.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 工艺路线（对应《设计规格说明书》5.2 WORKFLOW 表）。
 * 同一产品多版本（product_id + version 唯一），同一时刻至多一个 ACTIVE 版本；
 * 激活新版本时旧 ACTIVE 自动归档（ARCHIVED）。
 */
@Getter
@Setter
@Entity
@Table(name = "WORKFLOW", schema = DbSchema.NAME)
public class Workflow extends BaseEntity {

    /** 关联产品 ID */
    @Column(name = "PRODUCT_ID", length = 36, nullable = false)
    private String productId;

    /** 版本号（产品内自增：1, 2, 3...） */
    @Column(name = "VERSION", nullable = false)
    private Integer version;

    /** 版本标识（如 "V1.0"，默认 V{version}.0） */
    @Column(name = "VERSION_NAME", length = 32, nullable = false)
    private String versionName;

    /** 版本说明 */
    @Column(name = "DESCRIPTION", length = 255)
    private String description;

    /** 是否当前生效版本 */
    @Column(name = "IS_ACTIVE", nullable = false)
    private Boolean isActive = false;

    /** 状态：DRAFT / ACTIVE / ARCHIVED */
    @Column(name = "STATUS", length = 16, nullable = false)
    private String status;
}
