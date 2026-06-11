package com.blog.entity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@EqualsAndHashCode(of = "id")
public class Tag {
    private Long id;
    private String name;
    private String slug;
    private LocalDateTime createdAt;

    /** 瞬态字段：批量查询时用于关联 postId（非数据库列） */
    private transient Long postId;
}
