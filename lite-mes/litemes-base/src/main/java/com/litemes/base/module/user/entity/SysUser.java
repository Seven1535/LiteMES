package com.litemes.base.module.user.entity;

import com.litemes.common.core.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 系统用户（对应《设计规格说明书》5.2 SYS_USER 表）。
 * 角色：ADMIN=管理员（全部功能），OPERATOR=操作工（看板+我的任务）。
 * 状态：ENABLED=启用，DISABLED=停用。
 */
@Getter
@Setter
@Entity
@Table(name = "SYS_USER")
public class SysUser extends BaseEntity {

    /** 登录账号（唯一，业务层保证） */
    @Column(name = "USERNAME", length = 32, nullable = false)
    private String username;

    /** BCrypt 加密后的密码，任何 VO/DTO 禁止外泄 */
    @Column(name = "PASSWORD", length = 100, nullable = false)
    private String password;

    /** 真实姓名 */
    @Column(name = "REAL_NAME", length = 64, nullable = false)
    private String realName;

    /** 角色：ADMIN / OPERATOR */
    @Column(name = "ROLE", length = 16, nullable = false)
    private String role;

    /** 状态：ENABLED / DISABLED */
    @Column(name = "STATUS", length = 16, nullable = false)
    private String status;
}
