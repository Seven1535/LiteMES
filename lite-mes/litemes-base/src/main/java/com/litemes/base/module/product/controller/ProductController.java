package com.litemes.base.module.product.controller;

import com.litemes.base.module.product.dto.ProductCreateRequest;
import com.litemes.base.module.product.dto.ProductQueryRequest;
import com.litemes.base.module.product.dto.ProductUpdateRequest;
import com.litemes.base.module.product.dto.ProductVO;
import com.litemes.base.module.product.service.ProductService;
import com.litemes.common.core.AjaxResult;
import com.litemes.common.core.PageResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 产品管理接口（经网关：/api/v1/base/products，仅 ADMIN 可访问由前端菜单控制）。
 */
@RestController
@RequestMapping("/api/v1/base/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /** 分页查询（编码/名称模糊 + 状态精确） */
    @GetMapping
    public AjaxResult page(@Valid ProductQueryRequest query) {
        PageResult<ProductVO> result = productService.page(query);
        return AjaxResult.success(result);
    }

    /** 产品详情 */
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable String id) {
        return AjaxResult.success(productService.getById(id));
    }

    /** 新增产品 */
    @PostMapping
    public AjaxResult create(@Valid @RequestBody ProductCreateRequest request) {
        return AjaxResult.success(productService.create(request));
    }

    /** 修改产品（编码不可改） */
    @PutMapping("/{id}")
    public AjaxResult update(@PathVariable String id, @Valid @RequestBody ProductUpdateRequest request) {
        return AjaxResult.success(productService.update(id, request));
    }

    /** 删除产品（逻辑删除，被工单引用时返回 400） */
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable String id) {
        productService.delete(id);
        return AjaxResult.success(null);
    }
}
