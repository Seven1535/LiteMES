package com.litemes.base.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 修改用户请求（用户名不可改，密码走重置接口） */
@Data
public class UserUpdateRequest {

    @NotBlank(message = "姓名不能为空")
    @Size(max = 64, message = "姓名长度不能超过 64 位")
    private String realName;

    @NotBlank(message = "角色不能为空")
    @Pattern(regexp = "ADMIN|OPERATOR", message = "角色只能是 ADMIN 或 OPERATOR")
    private String role;

    @NotBlank(message = "状态不能为空")
    @Pattern(regexp = "ENABLED|DISABLED", message = "状态只能是 ENABLED 或 DISABLED")
    private String status;
}
