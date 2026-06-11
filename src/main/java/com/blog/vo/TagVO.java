package com.blog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "标签信息")
public class TagVO {

    @Schema(description = "标签 ID")
    private Long id;

    @Schema(description = "标签名称")
    private String name;

    @Schema(description = "URL 别名")
    private String slug;

    @Schema(description = "文章数量")
    private Integer postCount;
}
