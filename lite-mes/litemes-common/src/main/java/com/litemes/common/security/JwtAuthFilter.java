package com.litemes.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.litemes.common.config.JpaAuditConfig.JwtConstants;
import com.litemes.common.config.SecurityProperties;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 下游服务兜底鉴权过滤器（第二道防线，第一道是网关 AuthGlobalFilter）。
 * 逻辑（见《架构设计说明书》3.5）：
 * 1. 白名单路径直接放行；
 * 2. 存在网关注入的身份 Header（X-User-Id）时信任内部流量，直接写入 request attribute；
 * 3. 否则校验 Bearer Token（覆盖 Feign 透传与服务被直连的场景）；
 * 4. 校验通过后把 userId / role 写入 request attribute，供审计与业务使用。
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;
    private final SecurityProperties securityProperties;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return securityProperties.getWhitelist().stream().anyMatch(pattern -> pathMatcher(pattern, path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 1. 网关注入的身份 Header：内部流量，直接信任
        String headerUserId = request.getHeader(JwtConstants.HEADER_USER_ID);
        if (headerUserId != null && !headerUserId.isBlank()) {
            writeIdentity(request, headerUserId, request.getHeader(JwtConstants.HEADER_USER_ROLE));
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Bearer Token 校验（Feign 透传 / 服务被直连）
        String authHeader = request.getHeader(JwtConstants.HEADER_AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(JwtConstants.BEARER_PREFIX)) {
            unauthorized(response);
            return;
        }
        String token = authHeader.substring(JwtConstants.BEARER_PREFIX.length());
        if (!jwtTokenProvider.validateToken(token)) {
            unauthorized(response);
            return;
        }

        Claims claims = jwtTokenProvider.parseToken(token);
        writeIdentity(request, claims.getSubject(), claims.get("role", String.class));
        filterChain.doFilter(request, response);
    }

    private void writeIdentity(HttpServletRequest request, String userId, String role) {
        request.setAttribute(JwtConstants.ATTR_USER_ID, userId);
        request.setAttribute(JwtConstants.ATTR_ROLE, role);
    }

    private void unauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"未认证或登录已过期\",\"data\":null}");
    }

    /** 简易 ANT 风格匹配（* 匹配任意段，** 匹配任意路径） */
    private boolean pathMatcher(String pattern, String path) {
        if ("**".equals(pattern) || pattern.equals(path)) {
            return true;
        }
        String regex = pattern
                .replace("**", ".*")
                .replace("*", "[^/]*");
        return path.matches(regex);
    }
}
