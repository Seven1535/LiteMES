package com.litemes.common.security;

import com.litemes.common.config.JpaAuditConfig.JwtConstants;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Feign 身份透传拦截器（见《架构设计说明书》3.5）：
 * 服务间调用不走网关，把当前请求的身份信息写入 Feign 请求 Header，
 * 被调服务的 JwtAuthFilter 按"内部流量信任 Header"处理，保证调用链上身份不丢失。
 */
public class FeignRequestInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }
        Object userId = attributes.getAttribute(JwtConstants.ATTR_USER_ID, RequestAttributes.SCOPE_REQUEST);
        Object role = attributes.getAttribute(JwtConstants.ATTR_ROLE, RequestAttributes.SCOPE_REQUEST);
        if (userId != null) {
            template.header(JwtConstants.HEADER_USER_ID, userId.toString());
        }
        if (role != null) {
            template.header(JwtConstants.HEADER_USER_ROLE, role.toString());
        }
    }
}
