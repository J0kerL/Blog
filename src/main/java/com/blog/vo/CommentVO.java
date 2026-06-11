package com.blog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@ToString
@Schema(description = "评论信息")
public class CommentVO {

    @Schema(description = "评论 ID")
    private Long id;

    @Schema(description = "文章 ID")
    private Long postId;

    @Schema(description = "父评论 ID")
    private Long parentId;

    @Schema(description = "评论内容")
    private String content;

    @Schema(description = "评论者昵称")
    private String nickname;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "评论时间")
    private LocalDateTime createdAt;

    @Schema(description = "子评论列表")
    private List<CommentVO> replies;
}
