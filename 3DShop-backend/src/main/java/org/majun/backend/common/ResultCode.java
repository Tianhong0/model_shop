package org.majun.backend.common;

import lombok.Getter;

/**
 * 返回状态码枚举
 */
@Getter
public enum ResultCode {

    /**
     * 成功
     */
    SUCCESS(200, "操作成功"),

    /**
     * 失败
     */
    FAIL(500, "操作失败"),

    /**
     * 参数校验失败
     */
    PARAM_ERROR(400, "参数校验失败"),

    /**
     * 未授权
     */
    UNAUTHORIZED(401, "未授权，请先登录"),

    /**
     * 禁止访问
     */
    FORBIDDEN(403, "禁止访问"),

    /**
     * 资源不存在
     */
    NOT_FOUND(404, "资源不存在"),

    /**
     * 方法不支持
     */
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),

    /**
     * 系统繁忙
     */
    SYSTEM_ERROR(503, "系统繁忙，请稍后再试"),

    /**
     * 用户名或密码错误
     */
    LOGIN_ERROR(1001, "用户名或密码错误"),

    /**
     * 用户已被禁用
     */
    USER_DISABLED(1002, "用户已被禁用"),

    /**
     * 用户不存在
     */
    USER_NOT_FOUND(1003, "用户不存在"),

    /**
     * 用户已存在
     */
    USER_ALREADY_EXISTS(1004, "用户已存在"),

    /**
     * 密码错误
     */
    PASSWORD_ERROR(1005, "密码错误"),

    /**
     * 原密码错误
     */
    OLD_PASSWORD_ERROR(1006, "原密码错误"),

    /**
     * Token无效或已过期
     */
    TOKEN_INVALID(1007, "Token无效或已过期"),

    /**
     * Token已过期
     */
    TOKEN_EXPIRED(1008, "Token已过期，请重新登录"),

    /**
     * 验证码错误
     */
    CAPTCHA_ERROR(1009, "验证码错误"),

    /**
     * 验证码已过期
     */
    CAPTCHA_EXPIRED(1010, "验证码已过期"),

    /**
     * 手机号格式错误
     */
    PHONE_FORMAT_ERROR(1011, "手机号格式错误"),

    /**
     * 邮箱格式错误
     */
    EMAIL_FORMAT_ERROR(1012, "邮箱格式错误"),

    /**
     * 角色不存在
     */
    ROLE_NOT_FOUND(1013, "角色不存在"),

    /**
     * 权限不足
     */
    PERMISSION_DENIED(1014, "权限不足"),

    /**
     * 删除请求已存在
     */
    DELETION_REQUEST_EXISTS(1015, "删除请求已存在"),

    /**
     * 删除请求不存在
     */
    DELETION_REQUEST_NOT_FOUND(1016, "删除请求不存在");

    private final Integer code;

    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

}
