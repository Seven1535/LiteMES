package com.litemes.base.feign;

import com.litemes.api.base.dto.ProductDTO;
import com.litemes.base.module.product.entity.Product;
import com.litemes.base.module.product.repository.ProductRepository;
import com.litemes.common.core.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 产品内部接口：实现 litemes-api 的 ProductClient 契约（供 litemes-production 创建工单时校验产品）。
 * 不经网关，由 JwtAuthFilter 识别 Feign 透传的 X-User-Id Header 放行。
 */
@RestController
@RequestMapping("/inner")
@RequiredArgsConstructor
public class ProductInnerController {

    private static final String DEL_FLAG_NORMAL = "0";

    private final ProductRepository productRepository;

    /** 对应契约：GET /inner/products/{id} */
    @GetMapping("/products/{id}")
    public ProductDTO getProduct(@PathVariable String id) {
        Product product = productRepository.findById(id)
                .filter(p -> DEL_FLAG_NORMAL.equals(p.getDelFlag()))
                .orElseThrow(() -> new BusinessException(404, "产品不存在"));
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setProductCode(product.getProductCode());
        dto.setProductName(product.getProductName());
        dto.setDescription(product.getDescription());
        dto.setStatus(product.getStatus());
        return dto;
    }
}
