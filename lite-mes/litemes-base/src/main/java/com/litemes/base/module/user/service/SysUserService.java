package com.litemes.base.module.user.service;

import com.litemes.base.module.user.dto.ResetPasswordRequest;
import com.litemes.base.module.user.dto.UserCreateRequest;
import com.litemes.base.module.user.dto.UserQueryRequest;
import com.litemes.base.module.user.dto.UserUpdateRequest;
import com.litemes.base.module.user.dto.UserVO;
import com.litemes.base.module.user.entity.SysUser;
import com.litemes.base.module.user.repository.SysUserRepository;
import com.litemes.common.config.JpaAuditConfig.JwtConstants;
import com.litemes.common.core.BusinessException;
import com.litemes.common.core.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.List;
import java.util.UUID;

/**
 * 用户管理（管理员功能）：分页查询、新增、修改、逻辑删除、重置密码。
 * 业务规则：用户名唯一；不能删除自己；不能删除最后一个管理员。
 */
@Service
@RequiredArgsConstructor
public class SysUserService {

    private static final String DEL_FLAG_NORMAL = "0";
    private static final String DEL_FLAG_DELETED = "1";
    private static final String STATUS_ENABLED = "ENABLED";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String DEFAULT_PASSWORD = "123456";

    private final SysUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /** 分页查询（支持用户名模糊 + 角色精确筛选） */
    public PageResult<UserVO> page(UserQueryRequest query) {
        Pageable pageable = PageRequest.of(query.getPageNum() - 1, query.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        boolean hasUsername = query.getUsername() != null && !query.getUsername().isBlank();
        boolean hasRole = query.getRole() != null && !query.getRole().isBlank();

        Page<SysUser> page;
        if (hasRole && hasUsername) {
            page = userRepository.findByDelFlagAndRoleAndUsernameContaining(
                    DEL_FLAG_NORMAL, query.getRole(), query.getUsername().trim(), pageable);
        } else if (hasRole) {
            page = userRepository.findByDelFlagAndRole(DEL_FLAG_NORMAL, query.getRole(), pageable);
        } else if (hasUsername) {
            page = userRepository.findByDelFlagAndUsernameContaining(
                    DEL_FLAG_NORMAL, query.getUsername().trim(), pageable);
        } else {
            page = userRepository.findByDelFlag(DEL_FLAG_NORMAL, pageable);
        }

        List<UserVO> rows = page.getContent().stream().map(this::toVO).toList();
        return PageResult.of(rows, page.getTotalElements(), query.getPageNum(), query.getPageSize());
    }

    public UserVO getById(String id) {
        return toVO(getOrThrow(id));
    }

    /** 启用状态用户简表（派工选操作员等下拉场景，不分页） */
    public List<UserVO> listEnabled() {
        return userRepository.findByDelFlagAndStatus(DEL_FLAG_NORMAL, STATUS_ENABLED).stream()
                .map(this::toVO).toList();
    }

    @Transactional
    public UserVO create(UserCreateRequest request) {
        if (userRepository.existsByUsernameAndDelFlag(request.getUsername(), DEL_FLAG_NORMAL)) {
            throw new BusinessException("用户名已存在");
        }
        SysUser user = new SysUser();
        user.setId(UUID.randomUUID().toString());
        user.setUsername(request.getUsername());
        String rawPassword = request.getPassword() == null || request.getPassword().isBlank()
                ? DEFAULT_PASSWORD : request.getPassword();
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRealName(request.getRealName());
        user.setRole(request.getRole());
        user.setStatus(STATUS_ENABLED);
        return toVO(userRepository.save(user));
    }

    @Transactional
    public UserVO update(String id, UserUpdateRequest request) {
        SysUser user = getOrThrow(id);
        // 不能停用/降权自己，避免当前会话把自己锁死
        if (id.equals(currentUserId()) && !"ADMIN".equals(request.getRole())) {
            throw new BusinessException("不能修改自己的管理员角色");
        }
        if (id.equals(currentUserId()) && !"ENABLED".equals(request.getStatus())) {
            throw new BusinessException("不能停用自己的账号");
        }
        // 最后一个管理员保护：改角色后系统至少要保留一个 ADMIN
        if (ROLE_ADMIN.equals(user.getRole()) && !ROLE_ADMIN.equals(request.getRole())
                && userRepository.countByRoleAndDelFlag(ROLE_ADMIN, DEL_FLAG_NORMAL) <= 1) {
            throw new BusinessException("系统至少需要保留一个管理员");
        }
        user.setRealName(request.getRealName());
        user.setRole(request.getRole());
        user.setStatus(request.getStatus());
        return toVO(userRepository.save(user));
    }

    /** 逻辑删除 */
    @Transactional
    public void delete(String id) {
        SysUser user = getOrThrow(id);
        if (id.equals(currentUserId())) {
            throw new BusinessException("不能删除当前登录的账号");
        }
        if (ROLE_ADMIN.equals(user.getRole())
                && userRepository.countByRoleAndDelFlag(ROLE_ADMIN, DEL_FLAG_NORMAL) <= 1) {
            throw new BusinessException("系统至少需要保留一个管理员");
        }
        user.setDelFlag(DEL_FLAG_DELETED);
        userRepository.save(user);
    }

    @Transactional
    public void resetPassword(String id, ResetPasswordRequest request) {
        SysUser user = getOrThrow(id);
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private SysUser getOrThrow(String id) {
        return userRepository.findById(id)
                .filter(u -> DEL_FLAG_NORMAL.equals(u.getDelFlag()))
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
    }

    /** 当前登录用户 ID（由 JwtAuthFilter 写入 request attribute） */
    private String currentUserId() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        Object userId = attributes == null ? null
                : attributes.getAttribute(JwtConstants.ATTR_USER_ID, RequestAttributes.SCOPE_REQUEST);
        return userId == null ? null : userId.toString();
    }

    private UserVO toVO(SysUser user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setUpdatedAt(user.getUpdatedAt());
        return vo;
    }
}
