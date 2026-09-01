package com.litemes.base.module.user.repository;

import com.litemes.base.module.user.entity.SysUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 用户仓库：所有查询默认过滤 delFlag="0"（逻辑删除）。
 */
public interface SysUserRepository extends JpaRepository<SysUser, String> {

    Optional<SysUser> findByUsernameAndDelFlag(String username, String delFlag);

    boolean existsByUsernameAndDelFlag(String username, String delFlag);

    long countByRoleAndDelFlag(String role, String delFlag);

    Page<SysUser> findByDelFlag(String delFlag, Pageable pageable);

    Page<SysUser> findByDelFlagAndUsernameContaining(String delFlag, String username, Pageable pageable);

    Page<SysUser> findByDelFlagAndRole(String delFlag, String role, Pageable pageable);

    Page<SysUser> findByDelFlagAndRoleAndUsernameContaining(String delFlag, String role, String username, Pageable pageable);

    /** 启用状态用户列表（派工选操作员等下拉场景） */
    List<SysUser> findByDelFlagAndStatus(String delFlag, String status);
}
