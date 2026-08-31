package com.litemes.base.module.workcenter.entity;

import com.litemes.base.config.DbSchema;
import com.litemes.common.core.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 工位（对应《设计规格说明书》5.2 WORK_CENTER 表）。
 * 状态：IDLE=空闲 / BUSY=忙碌（有进行中任务） / OFFLINE=离线（停用），默认 IDLE。
 * operatorId 由派工/报工业务驱动更新，基础数据页面不手工维护。
 */
@Getter
@Setter
@Entity
@Table(name = "WORK_CENTER", schema = DbSchema.NAME)
public class WorkCenter extends BaseEntity {

    /** 工位编码（唯一，业务层保证） */
    @Column(name = "CENTER_CODE", length = 32, nullable = false)
    private String centerCode;

    /** 工位名称 */
    @Column(name = "CENTER_NAME", length = 128, nullable = false)
    private String centerName;

    /** 工位类型（如"车床"、"铣床"、"检验台"） */
    @Column(name = "CENTER_TYPE", length = 32)
    private String centerType;

    /** 当前操作员 ID（派工时写入，可为空） */
    @Column(name = "OPERATOR_ID", length = 64)
    private String operatorId;

    /** 状态：IDLE / BUSY / OFFLINE */
    @Column(name = "STATUS", length = 16, nullable = false)
    private String status;
}
