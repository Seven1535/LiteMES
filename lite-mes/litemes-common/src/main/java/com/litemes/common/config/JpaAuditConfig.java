package com.litemes.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Optional;

/**
 * JPA 审计配置：为 BaseEntity 的 createdBy / updatedBy 自动填充当前操作人。
 * 身份由 JwtAuthFilter 写入 request attribute，Feign 内部调用时由 FeignRequestInterceptor 透传。
 */
@Configuration
public class JpaAuditConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return Optional.empty();
            }
            Object userId = attributes.getAttribute(JwtConstants.ATTR_USER_ID,
                    RequestAttributes.SCOPE_REQUEST);
            return Optional.ofNullable(userId == null ? null : userId.toString());
        };
    }

    /** 身份信息常量：request attribute 与 Header 名称统一收口 */
    public static final class JwtConstants {
        public static final String ATTR_USER_ID = "userId";
        public static final String ATTR_ROLE = "role";
        public static final String HEADER_USER_ID = "X-User-Id";
        public static final String HEADER_USER_ROLE = "X-User-Role";
        public static final String HEADER_AUTHORIZATION = "Authorization";
        public static final String BEARER_PREFIX = "Bearer ";

        private JwtConstants() {
        }
    }
}
