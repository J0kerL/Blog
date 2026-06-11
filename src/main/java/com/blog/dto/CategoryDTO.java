package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "分类创建/更新请求")
public class CategoryDTO {

    @NotBlank(message = "分类名称不能为空")
    @Schema(description = "分类名称", example = "技术")
    private String name;

    @Schema(description = "URL 别名", example = "tech")
    private String slug;

    @Schema(description = "分类描述")
    private String description;

    @Schema(description = "排序", example = "0")
    private Integer sortOrder;
}
