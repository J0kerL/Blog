package com.blog.entity;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class Comment {
    private Long id;
    private Long postId;
    private Long parentId;        // 父评论 ID，顶级为 null
    private Long userId;
    private String nickname;       // 游客昵称（未登录时）
    private String email;          // 游客邮箱
    private String content;
    private Integer status;        // 0=待审核 1=已通过 2=已拒绝
    private LocalDateTime createdAt;

    // 非数据库字段
    private User user;
    private List<Comment> replies;
}
