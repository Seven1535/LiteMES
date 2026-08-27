package com.litemes.base.module.auth.controller;

import com.litemes.base.module.auth.dto.ChangePasswordRequest;
import com.litemes.base.module.auth.dto.LoginRequest;
import com.litemes.base.module.auth.dto.LoginResult;
import com.litemes.base.module.auth.service.AuthService;
import com.litemes.common.config.JpaAuditConfig.JwtConstants;
import com.litemes.common.core.AjaxResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口。/login 在网关与下游白名单中免鉴权；/logout、/change-password 需要有效 Token。
 */
@RestController
@RequestMapping("/api/v1/base/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 登录（免鉴权） */
    @PostMapping("/login")
    public AjaxResult<LoginResult> login(@Valid @RequestBody LoginRequest request) {
        return AjaxResult.success(authService.login(request));
    }

    /** 登出：Token 拉黑，网关立即拒绝该 Token 的后续请求 */
    @PostMapping("/logout")
    public AjaxResult<Void> logout(HttpServletRequest request) {
        String authHeader = request.getHeader(JwtConstants.HEADER_AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith(JwtConstants.BEARER_PREFIX)) {
            authService.logout(authHeader.substring(JwtConstants.BEARER_PREFIX.length()));
        }
        return AjaxResult.success();
    }

    /** 修改密码（需校验旧密码） */
    @PostMapping("/change-password")
    public AjaxResult<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return AjaxResult.success();
    }
}
