package com.blog.entity;

import lombok.Data;

@Data
public class PostCategory {
    private Long id;
    private Long postId;
    private Long categoryId;
}
