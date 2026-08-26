package com.litemes.common.core;

import lombok.Getter;

/**
 * 业务异常：业务规则校验失败时抛出，由 GlobalExceptionHandler 统一转为 AjaxResult。
 * 禁止在业务代码中抛裸 RuntimeException。
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 错误码，默认 400（参数/业务规则不满足）；资源不存在传 404 */
    private final int code;

    public BusinessException(String message) {
        this(400, message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
