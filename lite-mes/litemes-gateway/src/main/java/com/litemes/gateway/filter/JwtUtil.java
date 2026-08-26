package com.litemes.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * 网关侧 JWT 校验工具（与 litemes-common 的 JwtTokenProvider 保持同一套签名参数）。
 * 网关是 WebFlux 环境，不依赖 common 模块，故自持一份校验逻辑。
 */
public final class JwtUtil {

    private JwtUtil() {
    }

    /** 解析并校验 Token（签名 + 有效期），失败抛 JwtException */
    public static Claims parseToken(String token, String secret) {
        return Jwts.parser()
                .verifyWith(secretKey(secret))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public static boolean isValid(String token, String secret) {
        try {
            parseToken(token, secret);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private static SecretKey secretKey(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
