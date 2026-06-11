package com.blog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "Admin-用户信息（含状态）")
public class AdminUserVO {

    @Schema(description = "用户 ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "头像 URL")
    private String avatar;

    @Schema(description = "个人简介")
    private String bio;

    @Schema(description = "角色")
    private String role;

    @Schema(description = "状态：0=禁用 1=正常")
    private Integer status;

    @Schema(description = "注册时间")
    private LocalDateTime createdAt;
}
