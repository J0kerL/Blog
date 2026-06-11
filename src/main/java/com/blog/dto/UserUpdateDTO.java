package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "用户信息更新请求")
public class UserUpdateDTO {

    @Size(max = 50, message = "昵称最长 50 个字符")
    @Schema(description = "昵称")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱")
    private String email;

    @Size(max = 500, message = "简介最长 500 个字符")
    @Schema(description = "个人简介")
    private String bio;
}
