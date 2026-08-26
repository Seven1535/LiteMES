package com.litemes.base;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * 基础数据服务入口（8081）。
 * 职责：系统管理（用户/角色/JWT 签发）、产品管理、工艺建模、工位管理，数据库 litemes_base。
 * 说明：
 * - scanBasePackages 覆盖 com.litemes，使 common 模块的公共组件（异常处理/鉴权过滤/Redis 等）生效；
 * - 服务间调用契约扫描 com.litemes.api（litemes-api 模块）。
 */
@SpringBootApplication(scanBasePackages = "com.litemes")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.litemes.api")
@EnableJpaAuditing
public class BaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(BaseApplication.class, args);
    }
}
