package com.litemes.production.module.workorder.repository;

import com.litemes.production.module.workorder.entity.WorkOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 工单仓库：所有查询默认过滤 delFlag="0"（逻辑删除）。
 */
public interface WorkOrderRepository extends JpaRepository<WorkOrder, String> {

    /** 产品引用计数（跨服务删除保护，忽略逻辑删除：历史工单也算引用） */
    long countByProductId(String productId);

    Page<WorkOrder> findByDelFlag(String delFlag, Pageable pageable);

    Page<WorkOrder> findByDelFlagAndOrderNoContaining(String delFlag, String orderNo, Pageable pageable);

    Page<WorkOrder> findByDelFlagAndStatus(String delFlag, String status, Pageable pageable);

    Page<WorkOrder> findByDelFlagAndOrderNoContainingAndStatus(String delFlag, String orderNo, String status, Pageable pageable);
}
