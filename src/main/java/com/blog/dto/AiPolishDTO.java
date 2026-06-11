package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "AI 润色文章请求")
public class AiPolishDTO {

    @NotBlank(message = "文章内容不能为空")
    @Schema(description = "需要润色的 Markdown 内容")
    private String content;

    @Schema(description = "润色要求（可选）", example = "让语言更加生动活泼")
    private String instruction;
}
