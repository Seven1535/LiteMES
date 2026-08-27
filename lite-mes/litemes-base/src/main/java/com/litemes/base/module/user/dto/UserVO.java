package com.litemes.base.module.user.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 用户展示对象（不含密码等敏感字段，对外接口统一返回本对象） */
@Data
public class UserVO {

    private String id;
    private String username;
    private String realName;
    private String role;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
