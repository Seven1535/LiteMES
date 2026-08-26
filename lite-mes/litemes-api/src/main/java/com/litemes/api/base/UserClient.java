package com.litemes.api.base;

import com.litemes.api.base.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 用户契约（litemes-base 内部接口）。
 * 对应《设计规格说明书》7.2 内部接口表：GET /inner/users/{id}
 * 实现方：litemes-base；消费方：litemes-production（派工校验操作员）。
 */
@FeignClient(name = "litemes-base", path = "/inner", contextId = "userClient")
public interface UserClient {

    /** 用户详情（派工时校验操作员） */
    @GetMapping("/users/{id}")
    UserDTO getUser(@PathVariable("id") String id);
}
