package com.litemes.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 安全配置（前缀 litemes.security），由各服务的 Nacos / 本地配置下发。
 * 注意：YAML 列表无法用 @Value 绑定（只认逗号分隔字符串），必须走本类。
 */
@Data
@Component
@ConfigurationProperties(prefix = "litemes.security")
public class SecurityProperties {

    /** 免鉴权路径（ANT 风格） */
    private List<String> whitelist = new ArrayList<>();
}
