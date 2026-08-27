package com.litemes.base.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 新增用户请求 */
@Data
public class UserCreateRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 32, message = "用户名长度为 4-32 位")
    private String username;

    /** 初始密码，不填默认 123456 */
    @Size(min = 6, max = 32, message = "密码长度为 6-32 位")
    private String password;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 64, message = "姓名长度不能超过 64 位")
    private String realName;

    @NotBlank(message = "角色不能为空")
    @Pattern(regexp = "ADMIN|OPERATOR", message = "角色只能是 ADMIN 或 OPERATOR")
    private String role;
}
