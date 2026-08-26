package com.litemes.common.core;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 实体基类：所有数据库实体必须继承。
 * 主键（UUID）、审计字段、逻辑删除字段统一在此维护，禁止子类重复定义。
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    /** 主键：UUID 字符串（36 位），代码生成，禁止数据库自增 */
    @Id
    @Column(name = "ID", length = 36)
    private String id;

    @CreatedDate
    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(name = "CREATED_BY", length = 36)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "UPDATED_BY", length = 36)
    private String updatedBy;

    /** 逻辑删除：0=正常, 1=已删除。所有查询默认过滤 "0" */
    @Column(name = "DEL_FLAG", length = 1)
    private String delFlag = "0";
}
