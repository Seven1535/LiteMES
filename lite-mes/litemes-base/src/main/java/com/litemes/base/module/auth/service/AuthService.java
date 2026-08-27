package com.litemes.base.module.auth.service;

import com.litemes.base.module.auth.dto.ChangePasswordRequest;
import com.litemes.base.module.auth.dto.LoginRequest;
import com.litemes.base.module.auth.dto.LoginResult;
import com.litemes.base.module.user.dto.UserVO;
import com.litemes.base.module.user.entity.SysUser;
import com.litemes.base.module.user.repository.SysUserRepository;
import com.litemes.common.config.JpaAuditConfig.JwtConstants;
import com.litemes.common.core.BusinessException;
import com.litemes.common.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务：登录签发 JWT、登出拉黑（网关据此强制下线）、修改密码。
 * 登出策略：把 Token 写入 Redis 黑名单 jwt:blacklist:{token}，TTL=Token 剩余有效期，
 * 网关 AuthGlobalFilter 每次请求检查该 key，命中即 401。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    /** 与网关 AuthGlobalFilter.BLACKLIST_KEY_PREFIX 保持一致 */
    private static final String BLACKLIST_KEY_PREFIX = "jwt:blacklist:";
    private static final String DEL_FLAG_NORMAL = "0";
    private static final String STATUS_ENABLED = "ENABLED";

    private final SysUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${litemes.jwt.expire-hours:24}")
    private long expireHours;

    /** 登录：账号密码校验 → 签发 Token（用户名/密码错误返回统一文案，防止账号枚举） */
    public LoginResult login(LoginRequest request) {
        SysUser user = userRepository.findByUsernameAndDelFlag(request.getUsername(), DEL_FLAG_NORMAL)
                .orElseThrow(() -> new BusinessException("用户名或密码错误"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (!STATUS_ENABLED.equals(user.getStatus())) {
            throw new BusinessException("账号已停用，请联系管理员");
        }

        LoginResult result = new LoginResult();
        result.setToken(jwtTokenProvider.generateToken(user.getId(), user.getRole()));
        result.setExpireHours(expireHours);
        result.setUser(toVO(user));
        return result;
    }

    /** 登出：Token 加入黑名单，剩余有效期内网关拒绝放行 */
    public void logout(String token) {
        try {
            Claims claims = jwtTokenProvider.parseToken(token);
            long ttlMillis = claims.getExpiration().getTime() - new Date().getTime();
            if (ttlMillis > 0) {
                stringRedisTemplate.opsForValue()
                        .set(BLACKLIST_KEY_PREFIX + token, "1", ttlMillis, TimeUnit.MILLISECONDS);
            }
        } catch (JwtException | IllegalArgumentException e) {
            // Token 已失效（过期/非法）无需拉黑，前端清本地即可
            log.debug("登出时 Token 已失效，跳过黑名单写入");
        }
    }

    /** 修改密码：校验旧密码后更新（当前用户取自 JwtAuthFilter 写入的 request attribute） */
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        SysUser user = userRepository.findById(currentUserId())
                .orElseThrow(() -> new BusinessException(401, "未认证或登录已过期"));
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException("旧密码不正确");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private String currentUserId() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        Object userId = attributes == null ? null
                : attributes.getAttribute(JwtConstants.ATTR_USER_ID, RequestAttributes.SCOPE_REQUEST);
        return userId == null ? null : userId.toString();
    }

    private UserVO toVO(SysUser user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setUpdatedAt(user.getUpdatedAt());
        return vo;
    }
}
