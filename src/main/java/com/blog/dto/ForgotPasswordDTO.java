package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "忘记密码请求（需验证码）")
public class ForgotPasswordDTO {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Schema(description = "注册邮箱", example = "admin@example.com")
    private String email;

    @NotBlank(message = "验证码 key 不能为空")
    @Schema(description = "验证码 key")
    private String captchaKey;

    @NotBlank(message = "验证码不能为空")
    @Schema(description = "验证码", example = "xK3a")
    private String captchaCode;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度 6-50 个字符")
    @Schema(description = "新密码", example = "654321")
    private String newPassword;
}
