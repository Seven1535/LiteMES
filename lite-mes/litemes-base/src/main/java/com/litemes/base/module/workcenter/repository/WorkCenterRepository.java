package com.litemes.base.module.workcenter.repository;

import com.litemes.base.module.workcenter.entity.WorkCenter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 工位仓库：所有查询默认过滤 delFlag="0"（逻辑删除）。
 */
public interface WorkCenterRepository extends JpaRepository<WorkCenter, String> {

    boolean existsByCenterCodeAndDelFlag(String centerCode, String delFlag);

    List<WorkCenter> findByDelFlag(String delFlag);

    List<WorkCenter> findByDelFlagAndCenterType(String delFlag, String centerType);

    Page<WorkCenter> findByDelFlag(String delFlag, Pageable pageable);

    Page<WorkCenter> findByDelFlagAndCenterCodeContaining(String delFlag, String centerCode, Pageable pageable);

    Page<WorkCenter> findByDelFlagAndCenterNameContaining(String delFlag, String centerName, Pageable pageable);

    Page<WorkCenter> findByDelFlagAndStatus(String delFlag, String status, Pageable pageable);

    Page<WorkCenter> findByDelFlagAndCenterCodeContainingAndStatus(String delFlag, String centerCode, String status, Pageable pageable);

    Page<WorkCenter> findByDelFlagAndCenterNameContainingAndStatus(String delFlag, String centerName, String status, Pageable pageable);
}
