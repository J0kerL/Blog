package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "AI 对话请求")
public class AiChatDTO {

    @NotBlank(message = "消息不能为空")
    @Schema(description = "用户消息", example = "帮我列一个文章大纲")
    private String message;

    @Schema(description = "会话 ID（用于保持上下文）", example = "session-xxx")
    private String sessionId;
}
