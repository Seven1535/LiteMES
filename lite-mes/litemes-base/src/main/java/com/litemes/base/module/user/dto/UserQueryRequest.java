package com.litemes.base.module.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/** 用户分页查询参数 */
@Data
public class UserQueryRequest {

    @Min(value = 1, message = "页码从 1 开始")
    private int pageNum = 1;

    @Min(value = 1, message = "每页条数至少 1")
    @Max(value = 100, message = "每页条数最多 100")
    private int pageSize = 10;

    /** 用户名（模糊匹配，可选） */
    private String username;

    /** 角色（精确匹配，可选） */
    private String role;
}
