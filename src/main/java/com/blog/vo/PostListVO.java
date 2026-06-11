package com.blog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "文章列表项")
public class PostListVO {

    @Schema(description = "文章 ID")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "URL 别名")
    private String slug;

    @Schema(description = "摘要")
    private String summary;

    @Schema(description = "封面图")
    private String coverImage;

    @Schema(description = "是否置顶")
    private Integer isTop;

    @Schema(description = "发布状态：0=草稿，1=已发布，2=已下架")
    private Integer status;

    @Schema(description = "阅读量")
    private Long viewCount;

    @Schema(description = "发布时间")
    private LocalDateTime publishedAt;

    @Schema(description = "作者")
    private UserVO author;

    @Schema(description = "分类列表")
    private List<CategoryVO> categories;

    @Schema(description = "标签列表")
    private List<TagVO> tags;
}
