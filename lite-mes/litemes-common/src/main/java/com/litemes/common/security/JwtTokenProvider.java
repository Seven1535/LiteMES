package com.litemes.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 生成与校验（JJWT 0.12 API）。
 * 由 litemes-base / litemes-production 共用；网关侧不依赖本模块，自持一份校验逻辑（WebFlux 环境隔离）。
 */
@Component
public class JwtTokenProvider {

    /** 签名密钥（生产环境由 Nacos 配置中心下发，禁止硬编码） */
    @Value("${litemes.jwt.secret:litemes-default-secret-key-change-me-please}")
    private String secret;

    /** Token 有效期（小时） */
    @Value("${litemes.jwt.expire-hours:24}")
    private long expireHours;

    /**
     * 生成 Token（登录成功后调用）
     *
     * @param userId 用户 ID
     * @param role   角色编码（ADMIN/DISPATCHER/OPERATOR/VIEWER）
     */
    public String generateToken(String userId, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expireHours * 3600_000L);
        return Jwts.builder()
                .subject(userId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey())
                .compact();
    }

    /**
     * 解析并校验 Token，返回 Claims（校验签名与有效期）。
     * 校验失败抛 JwtException，由调用方转为 401。
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** 校验 Token 是否有效（签名 + 有效期） */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private SecretKey secretKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
