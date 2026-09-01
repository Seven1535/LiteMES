package com.litemes.common.jpa;

import org.hibernate.dialect.MySQLSqlAstTranslator;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.exec.spi.JdbcOperation;

/**
 * 达梦 SQL AST 翻译器：把分页子句渲染为内联字面量。
 * <p>
 * MySQL 方言的 {@link MySQLSqlAstTranslator#visitOffsetFetchClause(QueryPart)} 走
 * {@code renderCombinedLimitClause} 输出参数化的 {@code limit ?}，而达梦驱动不允许
 * 行限定位置使用绑定参数（报"查询使用值表达式作为参数"）；且达梦默认兼容模式不支持
 * MySQL 风格 LIMIT 语法，支持 SQL:2008 标准的 {@code OFFSET n ROWS FETCH NEXT m ROWS ONLY}。
 * 此处改为在翻译期直接取绑定值并内联标准行限定子句。
 */
public class DamengSqlAstTranslator<T extends JdbcOperation> extends MySQLSqlAstTranslator<T> {

    public DamengSqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
        super(sessionFactory, statement);
    }

    @Override
    public void visitOffsetFetchClause(QueryPart queryPart) {
        if (isRowNumberingCurrentQueryPart()) {
            return;
        }
        if (queryPart.isRoot() && hasLimit()) {
            // 根查询的 limit/offset 来自 QueryOptions，translate 时已知具体数值，直接内联。
            // 达梦支持 SQL:2008 标准行限定子句，不支持默认兼容模式下的 MySQL LIMIT
            final Number limitValue = getLimit().getMaxRows();
            final Number offsetValue = getLimit().getFirstRow();
            if (offsetValue != null && offsetValue.intValue() > 0) {
                appendSql(" offset ");
                appendSql(offsetValue.intValue());
                appendSql(" rows");
            }
            appendSql(" fetch next ");
            appendSql(limitValue.intValue());
            appendSql(" rows only");
        } else {
            // 子查询显式写的 fetch/offset 子句：沿用父类（含窗口函数仿真）
            super.visitOffsetFetchClause(queryPart);
        }
    }
}
