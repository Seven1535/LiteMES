package com.litemes.base.feign;

import com.litemes.api.base.dto.UserDTO;
import com.litemes.base.module.user.entity.SysUser;
import com.litemes.base.module.user.repository.SysUserRepository;
import com.litemes.common.core.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户内部接口：实现 litemes-api 的 UserClient 契约（供 litemes-production 派工校验操作员）。
 * 不经网关，由 JwtAuthFilter 识别 Feign 透传的 X-User-Id Header 放行。
 */
@RestController
@RequestMapping("/inner")
@RequiredArgsConstructor
public class UserInnerController {

    private static final String DEL_FLAG_NORMAL = "0";

    private final SysUserRepository userRepository;

    /** 对应契约：GET /inner/users/{id} */
    @GetMapping("/users/{id}")
    public UserDTO getUser(@PathVariable String id) {
        SysUser user = userRepository.findById(id)
                .filter(u -> DEL_FLAG_NORMAL.equals(u.getDelFlag()))
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setRealName(user.getRealName());
        dto.setRole(user.getRole());
        dto.setStatus(user.getStatus());
        return dto;
    }
}
