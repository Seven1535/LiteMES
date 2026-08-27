package com.litemes.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 网关安全配置（前缀 litemes.security），由 Nacos 的 litemes-gateway.yml 下发。
 * 网关不依赖 litemes-common（WebFlux/Servlet 隔离），故自带一份属性类。
 * 注意：YAML 列表无法用 @Value 绑定（只认逗号分隔字符串），必须走本类。
 */
@Data
@Component
@ConfigurationProperties(prefix = "litemes.security")
public class GatewaySecurityProperties {

    /** 鉴权白名单路径（ANT 风格） */
    private List<String> whitelist = new ArrayList<>();
}
