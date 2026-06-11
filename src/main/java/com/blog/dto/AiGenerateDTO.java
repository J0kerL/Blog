package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "AI 生成文章请求")
public class AiGenerateDTO {

    @NotBlank(message = "prompt 不能为空")
    @Schema(description = "生成提示", example = "写一篇关于 Spring Boot 3 新特性的技术博客")
    private String prompt;

    @Schema(description = "期望标题（可选）", example = "Spring Boot 3 新特性详解")
    private String title;

    @Schema(description = "文章长度: short/medium/long", example = "medium")
    private String length;

    @Schema(description = "写作风格: technical/casual/academic", example = "technical")
    private String style;
}
