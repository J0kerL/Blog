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
@Schema(description = "文章详情")
public class PostVO {

    @Schema(description = "文章 ID")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "URL 别名")
    private String slug;

    @Schema(description = "摘要")
    private String summary;

    @Schema(description = "Markdown 内容")
    private String content;

    @Schema(description = "封面图")
    private String coverImage;

    @Schema(description = "状态: 0=草稿 1=已发布 2=已下架")
    private Integer status;

    @Schema(description = "是否置顶")
    private Integer isTop;

    @Schema(description = "是否允许评论")
    private Integer allowComment;

    @Schema(description = "阅读量")
    private Long viewCount;

    @Schema(description = "发布时间")
    private LocalDateTime publishedAt;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "作者")
    private UserVO author;

    @Schema(description = "分类列表")
    private List<CategoryVO> categories;

    @Schema(description = "标签列表")
    private List<TagVO> tags;
}
