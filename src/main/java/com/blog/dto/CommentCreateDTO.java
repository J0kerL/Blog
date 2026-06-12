package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "提交评论请求")
public class CommentCreateDTO {

    @NotNull(message = "文章 ID 不能为空")
    @Schema(description = "文章 ID", example = "1")
    private Long postId;

    @Schema(description = "父评论 ID（顶级评论留空）", example = "null")
    private Long parentId;

    @Schema(description = "游客昵称（已登录用户可留空）", example = "访客A")
    private String nickname;

    @NotBlank(message = "评论内容不能为空")
    @Schema(description = "评论内容", example = "写得真好！")
    private String content;
}
