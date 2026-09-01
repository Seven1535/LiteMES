package com.litemes.base.module.user.controller;

import com.litemes.base.module.user.dto.ResetPasswordRequest;
import com.litemes.base.module.user.dto.UserCreateRequest;
import com.litemes.base.module.user.dto.UserQueryRequest;
import com.litemes.base.module.user.dto.UserUpdateRequest;
import com.litemes.base.module.user.dto.UserVO;
import com.litemes.base.module.user.service.SysUserService;
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

import java.util.List;

/**
 * 用户管理接口（管理员）。路径规范见《开发规范说明文档》3.1。
 */
@RestController
@RequestMapping("/api/v1/base/users")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService userService;

    /** 分页查询用户列表 */
    @GetMapping
    public AjaxResult<PageResult<UserVO>> page(@Valid UserQueryRequest query) {
        return AjaxResult.success(userService.page(query));
    }

    /** 启用状态用户简表（派工选操作员等下拉场景） */
    @GetMapping("/list")
    public AjaxResult<List<UserVO>> listEnabled() {
        return AjaxResult.success(userService.listEnabled());
    }

    /** 用户详情 */
    @GetMapping("/{id}")
    public AjaxResult<UserVO> detail(@PathVariable String id) {
        return AjaxResult.success(userService.getById(id));
    }

    /** 新增用户（初始密码不填默认 123456） */
    @PostMapping
    public AjaxResult<UserVO> create(@Valid @RequestBody UserCreateRequest request) {
        return AjaxResult.success(userService.create(request));
    }

    /** 修改用户（姓名/角色/状态，用户名不可改） */
    @PutMapping("/{id}")
    public AjaxResult<UserVO> update(@PathVariable String id,
                                     @Valid @RequestBody UserUpdateRequest request) {
        return AjaxResult.success(userService.update(id, request));
    }

    /** 删除用户（逻辑删除，被保护的管理员/自己不可删） */
    @DeleteMapping("/{id}")
    public AjaxResult<Void> delete(@PathVariable String id) {
        userService.delete(id);
        return AjaxResult.success();
    }

    /** 重置密码 */
    @PostMapping("/{id}/reset-password")
    public AjaxResult<Void> resetPassword(@PathVariable String id,
                                          @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(id, request);
        return AjaxResult.success();
    }
}
