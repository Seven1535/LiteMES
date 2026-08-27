package com.litemes.base.module.auth.dto;

import com.litemes.base.module.user.dto.UserVO;
import lombok.Data;

/** 登录成功返回（前端保存 token 并缓存 user 用于菜单角色过滤） */
@Data
public class LoginResult {

    /** JWT Token，后续请求放 Authorization: Bearer {token} */
    private String token;

    /** Token 有效期（小时） */
    private long expireHours;

    /** 当前用户信息（不含密码） */
    private UserVO user;
}
