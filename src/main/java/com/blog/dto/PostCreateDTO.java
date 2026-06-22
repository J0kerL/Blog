package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "创建/更新文章请求")
public class PostCreateDTO {

    @NotBlank(message = "文章标题不能为空")
    @Size(max = 200, message = "标题最长 200 个字符")
    @Schema(description = "标题", example = "Spring Boot 3 实战")
    private String title;

    @Size(max = 200, message = "slug 最长 200 个字符")
    @Schema(description = "URL 别名（自动生成如留空）", example = "spring-boot-3-guide")
    private String slug;

    @Size(max = 500, message = "摘要最长 500 个字符")
    @Schema(description = "文章摘要")
    private String summary;

    @NotBlank(message = "文章内容不能为空")
    @Schema(description = "Markdown 内容")
    private String content;

    @Schema(description = "封面图 URL")
    private String coverImage;

    @Schema(description = "状态: 0=草稿 1=发布", example = "1")
    private Integer status;

    @Schema(description = "是否置顶", example = "0")
    private Integer isTop;

    @Schema(description = "是否允许评论", example = "1")
    private Integer allowComment;

    @Schema(description = "定时发布时间")
    private LocalDateTime scheduledAt;

    @Schema(description = "分类 ID 列表")
    private List<Long> categoryIds;

    @Schema(description = "标签 ID 列表")
    private List<Long> tagIds;

    @Schema(description = "新建分类名称列表（自动创建并关联）")
    private List<String> newCategoryNames;

    @Schema(description = "新建标签名称列表（自动创建并关联）")
    private List<String> newTagNames;
}
