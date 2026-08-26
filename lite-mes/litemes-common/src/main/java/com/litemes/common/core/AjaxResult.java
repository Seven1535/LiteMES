package com.litemes.common.core;

import lombok.Data;

/**
 * 统一响应体（全系统所有接口的返回格式，见《开发规范说明文档》3.2）
 *
 * @param <T> 业务数据类型
 */
@Data
public class AjaxResult<T> {

    /** 200=成功, 400=参数错误, 401=未认证, 403=无权限, 404=资源不存在, 500=系统错误 */
    private int code;

    /** 提示信息 */
    private String message;

    /** 业务数据 */
    private T data;

    public static <T> AjaxResult<T> success() {
        return success(null);
    }

    public static <T> AjaxResult<T> success(T data) {
        AjaxResult<T> result = new AjaxResult<>();
        result.code = 200;
        result.message = "success";
        result.data = data;
        return result;
    }

    public static <T> AjaxResult<T> error(int code, String message) {
        AjaxResult<T> result = new AjaxResult<>();
        result.code = code;
        result.message = message;
        return result;
    }
}
