package com.litemes.base.config;

/**
 * 本服务的数据库 schema 常量（达梦：用户即 schema）。
 * 实体 @Table 必须显式指定 schema：本达梦实例为多项目共享，存在其他用户的同名表（如 SYS_USER）。
 * 配合 {@link com.litemes.common.jpa.DamengDialect}（NameQualifierSupport.SCHEMA），
 * DDL 与元数据探测均带 schema 限定，不会误命中其他用户的表。
 */
public final class DbSchema {

    public static final String NAME = "LITEMES_BASE";

    private DbSchema() {
    }
}
