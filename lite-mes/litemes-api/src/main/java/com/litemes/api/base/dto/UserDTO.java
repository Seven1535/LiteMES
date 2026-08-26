package com.litemes.api.base.dto;

import lombok.Data;

/** 用户契约 DTO（字段与《设计规格说明书》5.2 SYS_USER 表对齐，不含密码等敏感字段） */
@Data
public class UserDTO {

    private String id;
    private String username;
    private String realName;
    private String role;
    private String status;
}
