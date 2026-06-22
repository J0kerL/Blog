package com.blog.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或 token 已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // 业务错误码
    USER_ALREADY_EXISTS(1001, "用户名已存在"),
    USER_NOT_FOUND(1002, "用户不存在"),
    PASSWORD_ERROR(1003, "密码错误"),
    POST_NOT_FOUND(1004, "文章不存在"),
    CATEGORY_NOT_FOUND(1005, "分类不存在"),
    TAG_NOT_FOUND(1006, "标签不存在"),
    COMMENT_NOT_FOUND(1007, "评论不存在"),
    UPLOAD_FAILED(1008, "文件上传失败"),
    AI_REQUEST_FAILED(1009, "AI 服务请求失败"),
    RATE_LIMIT_EXCEEDED(1010, "请求过于频繁，请稍后再试"),
    CAPTCHA_EXPIRED(1011, "验证码已过期"),
    CAPTCHA_ERROR(1012, "验证码错误"),
    OLD_PASSWORD_ERROR(1013, "旧密码错误"),
    EMAIL_NOT_FOUND(1014, "邮箱未注册"),
    POST_NOT_OWNER(1015, "无权操作此文章");

    private final int code;
    private final String message;
}
