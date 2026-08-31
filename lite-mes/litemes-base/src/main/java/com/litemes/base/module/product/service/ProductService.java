package com.litemes.base.module.product.service;

import com.litemes.api.production.ReferenceCheckClient;
import com.litemes.base.module.product.dto.ProductCreateRequest;
import com.litemes.base.module.product.dto.ProductQueryRequest;
import com.litemes.base.module.product.dto.ProductUpdateRequest;
import com.litemes.base.module.product.dto.ProductVO;
import com.litemes.base.module.product.entity.Product;
import com.litemes.base.module.product.repository.ProductRepository;
import com.litemes.common.core.BusinessException;
import com.litemes.common.core.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 产品业务：标准分页查询 / 新增 / 修改 / 逻辑删除（删除前走 production 引用校验）。
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private static final String DEL_FLAG_NORMAL = "0";
    private static final String DEL_FLAG_DELETED = "1";
    private static final String STATUS_ACTIVE = "ACTIVE";

    private final ProductRepository productRepository;
    private final ReferenceCheckClient referenceCheckClient;

    /** 分页查询：编码/名称模糊（二选一，编码优先）+ 状态精确 */
    public PageResult<ProductVO> page(ProductQueryRequest query) {
        Pageable pageable = PageRequest.of(query.getPageNum() - 1, query.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        String code = trimToNull(query.getProductCode());
        String name = trimToNull(query.getProductName());
        String status = trimToNull(query.getStatus());

        Page<Product> page;
        if (code != null) {
            page = status != null
                    ? productRepository.findByDelFlagAndProductCodeContainingAndStatus(DEL_FLAG_NORMAL, code, status, pageable)
                    : productRepository.findByDelFlagAndProductCodeContaining(DEL_FLAG_NORMAL, code, pageable);
        } else if (name != null) {
            page = status != null
                    ? productRepository.findByDelFlagAndProductNameContainingAndStatus(DEL_FLAG_NORMAL, name, status, pageable)
                    : productRepository.findByDelFlagAndProductNameContaining(DEL_FLAG_NORMAL, name, pageable);
        } else if (status != null) {
            page = productRepository.findByDelFlagAndStatus(DEL_FLAG_NORMAL, status, pageable);
        } else {
            page = productRepository.findByDelFlag(DEL_FLAG_NORMAL, pageable);
        }
        return PageResult.of(page.getContent().stream().map(this::toVO).toList(),
                page.getTotalElements(), query.getPageNum(), query.getPageSize());
    }

    public ProductVO getById(String id) {
        return toVO(loadActive(id));
    }

    @Transactional
    public ProductVO create(ProductCreateRequest request) {
        String code = request.getProductCode().trim();
        if (productRepository.existsByProductCodeAndDelFlag(code, DEL_FLAG_NORMAL)) {
            throw new BusinessException(400, "产品编码已存在：" + code);
        }
        Product product = new Product();
        product.setId(UUID.randomUUID().toString());
        product.setProductCode(code);
        product.setProductName(request.getProductName().trim());
        product.setDescription(trimToNull(request.getDescription()));
        product.setDrawingUrl(trimToNull(request.getDrawingUrl()));
        product.setStatus(request.getStatus() != null ? request.getStatus() : STATUS_ACTIVE);
        return toVO(productRepository.save(product));
    }

    @Transactional
    public ProductVO update(String id, ProductUpdateRequest request) {
        Product product = loadActive(id);
        product.setProductName(request.getProductName().trim());
        product.setDescription(trimToNull(request.getDescription()));
        product.setDrawingUrl(trimToNull(request.getDrawingUrl()));
        product.setStatus(request.getStatus());
        return toVO(productRepository.save(product));
    }

    /**
     * 逻辑删除：删除保护见《设计规格说明书》8.4。
     * 通过 ReferenceCheckClient 反查 production 的工单引用，引用数 > 0 拒绝删除；
     * fallback 返回 null 表示 production 不可用（安全优先，同样拒绝删除）。
     */
    @Transactional
    public void delete(String id) {
        Product product = loadActive(id);
        Integer references = referenceCheckClient.countProductReferences(id);
        if (references == null) {
            throw new BusinessException(500, "生产服务暂不可用，无法校验引用，删除已拒绝");
        }
        if (references > 0) {
            throw new BusinessException(400, "该产品已被 " + references + " 个工单引用，不能删除");
        }
        product.setDelFlag(DEL_FLAG_DELETED);
        productRepository.save(product);
    }

    private Product loadActive(String id) {
        return productRepository.findById(id)
                .filter(p -> DEL_FLAG_NORMAL.equals(p.getDelFlag()))
                .orElseThrow(() -> new BusinessException(404, "产品不存在"));
    }

    private ProductVO toVO(Product product) {
        ProductVO vo = new ProductVO();
        vo.setId(product.getId());
        vo.setProductCode(product.getProductCode());
        vo.setProductName(product.getProductName());
        vo.setDescription(product.getDescription());
        vo.setDrawingUrl(product.getDrawingUrl());
        vo.setStatus(product.getStatus());
        vo.setCreatedAt(product.getCreatedAt());
        vo.setUpdatedAt(product.getUpdatedAt());
        return vo;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
