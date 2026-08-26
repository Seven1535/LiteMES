package com.litemes.base.config;

import com.litemes.common.security.FeignRequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 安全相关配置。
 * 说明：鉴权统一由网关 AuthGlobalFilter（第一道防线）+ common 的 JwtAuthFilter（兜底）完成，
 * 这里只提供 BCrypt 密码加密与 Feign 身份透传拦截器，不引入 Spring Security 过滤器链。
 * CORS 仅为本地直连调试兜底，生产环境由网关统一处理。
 */
@Configuration
public class SecurityConfig {

    /** BCrypt 密码加密（用户密码存储与校验） */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Feign 身份透传：服务间调用不走网关，把当前身份写入请求头 */
    @Bean
    public FeignRequestInterceptor feignRequestInterceptor() {
        return new FeignRequestInterceptor();
    }

    /** CORS 兜底配置（仅直连调试用，生产走网关收口） */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOriginPatterns("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true)
                        .maxAge(3600);
            }
        };
    }
}
