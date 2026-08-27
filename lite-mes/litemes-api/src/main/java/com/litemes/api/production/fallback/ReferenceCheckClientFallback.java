package com.litemes.api.production.fallback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import com.litemes.api.production.ReferenceCheckClient;

/**
 * ReferenceCheckClient 降级工厂（放在契约模块，供消费方扫描；需配置 feign.circuitbreaker.enabled=true）。
 * 策略见契约注释（安全优先）：production 不可用时返回 null，
 * 由调用方判定为"无法校验引用"并拒绝删除，而不是放行删除。
 */
@Slf4j
@Component
public class ReferenceCheckClientFallback implements FallbackFactory<ReferenceCheckClient> {

    @Override
    public ReferenceCheckClient create(Throwable cause) {
        log.warn("litemes-production 不可用，引用校验降级（删除将被拒绝）: {}", cause.getMessage());
        return new ReferenceCheckClient() {
            @Override
            public Integer countProductReferences(String productId) {
                return null;
            }

            @Override
            public Integer countWorkCenterTasks(String workCenterId) {
                return null;
            }
        };
    }
}
