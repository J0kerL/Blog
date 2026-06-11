package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "AI 建议请求")
public class AiSuggestDTO {

    @Schema(description = "文章标题", example = "Spring Boot 3 实战指南")
    private String title;

    @Schema(description = "文章内容（可选）")
    private String content;
}
