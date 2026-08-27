package com.litemes.gateway.filter;

import com.litemes.gateway.config.GatewaySecurityProperties;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * JWT 全局鉴权过滤器（第一道防线，见《架构设计说明书》3.5）：
 * 1. 白名单路径直接放行（登录、Swagger 等，配置在 Nacos）；
 * 2. WebSocket 握手无法自定义 Header，Token 放 query 参数；
 * 3. 校验签名与有效期，检查 Redis 黑名单（登出后强制下线）；
 * 4. 校验失败直接返回 401，请求不会到达下游服务；
 * 5. 校验通过后注入 X-User-Id / X-User-Role Header，按路由规则转发。
 */
@Slf4j
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final String TOKEN_QUERY_PARAM = "token";
    private static final String BLACKLIST_KEY_PREFIX = "jwt:blacklist:";

    @Value("${litemes.jwt.secret:litemes-default-secret-key-change-me-please}")
    private String jwtSecret;

    private final GatewaySecurityProperties securityProperties;
    private final ReactiveStringRedisTemplate redisTemplate;

    public AuthGlobalFilter(GatewaySecurityProperties securityProperties, ReactiveStringRedisTemplate redisTemplate) {
        this.securityProperties = securityProperties;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (isWhitelisted(path)) {
            return chain.filter(exchange);
        }

        // 提取 Token：Header 优先（常规请求），query 参数兜底（WebSocket 握手）
        String token = extractToken(exchange.getRequest());
        if (token == null) {
            return unauthorized(exchange);
        }
        if (!JwtUtil.isValid(token, jwtSecret)) {
            log.warn("网关鉴权失败（Token 无效）: {}", path);
            return unauthorized(exchange);
        }

        // Redis 黑名单检查：命中说明已登出，强制下线
        return redisTemplate.hasKey(BLACKLIST_KEY_PREFIX + token)
                .flatMap(blacklisted -> {
                    if (Boolean.TRUE.equals(blacklisted)) {
                        log.warn("网关鉴权失败（Token 已登出）: {}", path);
                        return unauthorized(exchange);
                    }
                    return forwardWithIdentity(exchange, chain, token);
                });
    }

    /** 注入身份 Header 后转发下游 */
    private Mono<Void> forwardWithIdentity(ServerWebExchange exchange, GatewayFilterChain chain, String token) {
        Claims claims = JwtUtil.parseToken(token, jwtSecret);
        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .header("X-User-Id", claims.getSubject())
                .header("X-User-Role", claims.get("role", String.class))
                .build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    private String extractToken(ServerHttpRequest request) {
        String auth = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        return request.getQueryParams().getFirst(TOKEN_QUERY_PARAM);
    }

    private boolean isWhitelisted(String path) {
        return securityProperties.getWhitelist().stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = "{\"code\":401,\"message\":\"未认证或登录已过期\",\"data\":null}"
                .getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(body);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // 早于路由转发（NettyRoutingFilter），晚于指标类过滤器
        return -100;
    }
}
