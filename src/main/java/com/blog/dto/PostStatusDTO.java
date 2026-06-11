package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "更新文章状态请求")
public class PostStatusDTO {

    @NotNull(message = "状态不能为空")
    @Schema(description = "状态: 0=草稿 1=发布 2=下架", example = "1")
    private Integer status;
}
