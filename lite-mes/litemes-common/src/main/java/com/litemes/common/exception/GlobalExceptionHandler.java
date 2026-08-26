package com.litemes.common.exception;

import com.litemes.common.core.AjaxResult;
import com.litemes.common.core.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：Controller 禁止自行 try-catch，所有异常统一在此转 AjaxResult。
 * 日志规则见《开发规范说明文档》3.6。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：返回自定义错误码与提示 */
    @ExceptionHandler(BusinessException.class)
    public AjaxResult<Void> handleBusiness(BusinessException e) {
        log.warn("业务校验失败: {}", e.getMessage());
        return AjaxResult.error(e.getCode(), e.getMessage());
    }

    /** 参数校验失败（@Valid）：取第一条错误提示 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public AjaxResult<Void> handleValidation(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "参数校验失败";
        log.warn("参数校验失败: {}", message);
        return AjaxResult.error(400, message);
    }

    /** 兜底异常：统一 500，不向调用方暴露堆栈 */
    @ExceptionHandler(Exception.class)
    public AjaxResult<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return AjaxResult.error(500, "系统内部错误，请稍后重试");
    }
}
