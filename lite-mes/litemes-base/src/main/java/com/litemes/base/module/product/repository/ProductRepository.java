package com.litemes.base.module.product.repository;

import com.litemes.base.module.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 产品仓库：所有查询默认过滤 delFlag="0"（逻辑删除）。
 */
public interface ProductRepository extends JpaRepository<Product, String> {

    boolean existsByProductCodeAndDelFlag(String productCode, String delFlag);

    Page<Product> findByDelFlag(String delFlag, Pageable pageable);

    Page<Product> findByDelFlagAndProductCodeContaining(String delFlag, String productCode, Pageable pageable);

    Page<Product> findByDelFlagAndProductNameContaining(String delFlag, String productName, Pageable pageable);

    Page<Product> findByDelFlagAndStatus(String delFlag, String status, Pageable pageable);

    Page<Product> findByDelFlagAndProductCodeContainingAndStatus(String delFlag, String productCode, String status, Pageable pageable);

    Page<Product> findByDelFlagAndProductNameContainingAndStatus(String delFlag, String productName, String status, Pageable pageable);
}
