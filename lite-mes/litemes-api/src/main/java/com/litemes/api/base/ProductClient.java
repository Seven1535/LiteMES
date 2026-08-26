package com.litemes.api.base;

import com.litemes.api.base.dto.ProductDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 产品契约（litemes-base 内部接口，仅供 Feign 调用，不对前端暴露）。
 * 对应《设计规格说明书》7.2 内部接口表：GET /inner/products/{id}
 * 实现方：litemes-base；消费方：litemes-production。
 */
@FeignClient(name = "litemes-base", path = "/inner", contextId = "productClient")
public interface ProductClient {

    /** 查询产品详情（工单创建时校验产品存在） */
    @GetMapping("/products/{id}")
    ProductDTO getProduct(@PathVariable("id") String id);
}
