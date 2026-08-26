package com.litemes.common.core;

import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 分页响应体（放在 AjaxResult.data 内，见《开发规范说明文档》3.2）
 *
 * @param <T> 行数据类型
 */
@Data
public class PageResult<T> {

    private List<T> rows;
    private long total;
    private int pageNum;
    private int pageSize;

    public static <T> PageResult<T> of(List<T> rows, long total, int pageNum, int pageSize) {
        PageResult<T> result = new PageResult<>();
        result.rows = rows;
        result.total = total;
        result.pageNum = pageNum;
        result.pageSize = pageSize;
        return result;
    }

    /** 由 Spring Data 分页对象转换 */
    public static <T> PageResult<T> of(Page<T> page) {
        return of(page.getContent(), page.getTotalElements(), page.getNumber() + 1, page.getSize());
    }
}
