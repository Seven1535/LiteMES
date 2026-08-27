package com.litemes.api.production;

import com.litemes.api.production.fallback.ReferenceCheckClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 引用计数契约（litemes-production 内部接口，唯一的反向调用：base → production）。
 * 对应《设计规格说明书》7.2 内部接口表与 8.4 删除保护表：
 * - GET /inner/references/products/{productId}     产品被工单引用计数（删除保护）
 * - GET /inner/references/workcenters/{workCenterId} 工位进行中任务数（删除保护）
 * 实现方：litemes-production；消费方：litemes-base。
 * 注意：调用方必须配置 fallback —— production 不可用时删除请求直接拒绝（安全优先）。
 */
@FeignClient(name = "litemes-production", path = "/inner/references", contextId = "referenceCheckClient",
        fallbackFactory = ReferenceCheckClientFallback.class)
public interface ReferenceCheckClient {

    /** 产品被工单引用的数量，> 0 时禁止删除产品 */
    @GetMapping("/products/{productId}")
    Integer countProductReferences(@PathVariable("productId") String productId);

    /** 工位进行中的派工任务数，> 0 时禁止删除工位 */
    @GetMapping("/workcenters/{workCenterId}")
    Integer countWorkCenterTasks(@PathVariable("workCenterId") String workCenterId);
}
