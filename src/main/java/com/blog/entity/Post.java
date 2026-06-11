package com.blog.entity;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class Post {
    private Long id;
    private Long userId;
    private String title;
    private String slug;
    private String summary;
    private String content;        // Markdown 内容
    private String coverImage;
    private Integer status;        // 0=草稿 1=已发布 2=已下架
    private Integer isTop;         // 0=否 1=置顶
    private Integer allowComment;  // 0=否 1=是
    private Long viewCount;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 非数据库字段
    private User author;
    private List<Category> categories;
    private List<Tag> tags;
}
