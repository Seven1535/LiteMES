package com.litemes.base.module.product.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/** 产品分页查询参数 */
@Data
public class ProductQueryRequest {

    @Min(value = 1, message = "页码从 1 开始")
    private int pageNum = 1;

    @Min(value = 1, message = "每页条数至少 1")
    @Max(value = 100, message = "每页条数最多 100")
    private int pageSize = 10;

    /** 产品编码（模糊匹配，可选） */
    private String productCode;

    /** 产品名称（模糊匹配，可选） */
    private String productName;

    /** 状态（精确匹配，可选）：ACTIVE / INACTIVE */
    private String status;
}
