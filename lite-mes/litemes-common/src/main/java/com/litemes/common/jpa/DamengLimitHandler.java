package com.litemes.common.jpa;

import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.query.spi.Limit;

/**
 * 达梦分页子句渲染器：把 LIMIT / OFFSET 以字面量内联到 SQL 中。
 * <p>
 * 达梦驱动不允许 LIMIT / OFFSET 位置使用绑定参数（报"查询使用值表达式作为参数"），
 * 因此不能沿用 MySQL 方言的参数化 {@code limit ?}，改为渲染
 * SQL:2008 标准行限定语法 {@code OFFSET n ROWS FETCH NEXT m ROWS ONLY}（达梦兼容）。
 */
public class DamengLimitHandler extends AbstractLimitHandler {

    public static final DamengLimitHandler INSTANCE = new DamengLimitHandler();

    @Override
    public String processSql(String sql, Limit limit) {
        if (limit.isEmpty() || limit.getMaxRows() == null) {
            return sql;
        }
        int firstRow = limit.getFirstRowJpa();
        int maxRows = limit.getMaxRowsJpa();
        StringBuilder sb = new StringBuilder(sql.length() + 40);
        sb.append(sql);
        if (firstRow > 0) {
            sb.append(" offset ").append(firstRow).append(" rows");
        }
        sb.append(" fetch next ").append(maxRows).append(" rows only");
        return sb.toString();
    }

    @Override
    public boolean supportsLimit() {
        return true;
    }

    @Override
    public boolean supportsOffset() {
        return true;
    }
}
