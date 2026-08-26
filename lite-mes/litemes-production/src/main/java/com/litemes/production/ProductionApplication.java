package com.litemes.production;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * 业务数据服务入口（8082，含 WebSocket）。
 * 职责：生产工单、派工管理、报工、生产看板（聚合 Feign 主数据），数据库 litemes_production。
 * 说明：
 * - scanBasePackages 覆盖 com.litemes，使 common 模块的公共组件生效；
 * - 服务间调用契约扫描 com.litemes.api（litemes-api 模块）。
 */
@SpringBootApplication(scanBasePackages = "com.litemes")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.litemes.api")
@EnableJpaAuditing
public class ProductionApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductionApplication.class, args);
    }
}
