package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "修改密码请求（已登录用户）")
public class ChangePasswordDTO {

    @NotBlank(message = "旧密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度 6-50 个字符")
    @Schema(description = "旧密码", example = "123456")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度 6-50 个字符")
    @Schema(description = "新密码", example = "654321")
    private String newPassword;
}
