package com.litemes.common.jpa;

import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;

/**
 * 达梦（DM8）方言：Hibernate 6 无内置达梦方言，基于 MySQL 方言修正两处不兼容。
 * <p>
 * 达梦语法大体兼容 MySQL，但：
 * <ol>
 *   <li>达梦是"用户即 schema"，而 MySQL 只有 catalog。{@link MySQLDialect#getNameQualifierSupport()}
 *       返回 CATALOG，导致 DDL 渲染时 {@code @Table(schema=...)} 的 schema 前缀被丢弃——
 *       在多项目共享的达梦实例上会误操作其他用户的同名表，必须改为 SCHEMA；</li>
 *   <li>MySQL 方言生成的建表语句带 {@code engine=InnoDB} 子句，达梦不识别，需去掉；</li>
 *   <li>MySQL 方言的分页子句为参数化 {@code limit ?}，达梦驱动不允许 LIMIT 位置用绑定参数：
 *       AST 查询路径由 {@link DamengSqlAstTranslator} 内联字面量，
 *       遗留查询路径由 {@link DamengLimitHandler} 内联字面量。</li>
 * </ol>
 * 配合 {@code ddl-auto=update} 使用：schema 限定生效后，元数据探测只查本用户的表，
 * 不会误命中同实例其他用户的同名表。
 */
public class DamengDialect extends MySQLDialect {

    @Override
    public NameQualifierSupport getNameQualifierSupport() {
        return NameQualifierSupport.SCHEMA;
    }

    @Override
    public String getTableTypeString() {
        return "";
    }

    @Override
    public LimitHandler getLimitHandler() {
        return DamengLimitHandler.INSTANCE;
    }

    /**
     * SQL AST 翻译器工厂：select/mutation 用达梦专用翻译器（分页内联字面量），
     * 其余（delete/insert/update/model mutation）沿用标准工厂。
     */
    private static final SqlAstTranslatorFactory DAMENG_TRANSLATOR_FACTORY = new StandardSqlAstTranslatorFactory() {
        @Override
        protected <T extends org.hibernate.sql.exec.spi.JdbcOperation> org.hibernate.sql.ast.SqlAstTranslator<T> buildTranslator(
                org.hibernate.engine.spi.SessionFactoryImplementor sessionFactory,
                org.hibernate.sql.ast.tree.Statement statement) {
            return new DamengSqlAstTranslator<>(sessionFactory, statement);
        }
    };

    @Override
    public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
        return DAMENG_TRANSLATOR_FACTORY;
    }
}
