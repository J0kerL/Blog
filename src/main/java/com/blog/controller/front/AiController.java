package com.blog.controller.front;

import com.blog.dto.AiChatDTO;
import com.blog.dto.AiGenerateDTO;
import com.blog.dto.AiPolishDTO;
import com.blog.dto.AiSuggestDTO;
import com.blog.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import com.blog.common.Result;
import reactor.core.publisher.Flux;

@Tag(name = "AI 写作助手", description = "基于 MiMo 大模型的 AI 写作功能")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @Operation(summary = "AI 生成文章", description = "根据 prompt 生成完整 Markdown 文章")
    @PostMapping("/generate-article")
    public Result<String> generateArticle(@Valid @RequestBody AiGenerateDTO dto) {
        return Result.ok(aiService.generateArticle(dto));
    }

    @Operation(summary = "AI 润色文章", description = "润色优化现有文章内容")
    @PostMapping("/polish")
    public Result<String> polish(@Valid @RequestBody AiPolishDTO dto) {
        return Result.ok(aiService.polishArticle(dto));
    }

    @Operation(summary = "AI 建议", description = "根据标题/内容建议标签、摘要、SEO 描述")
    @PostMapping("/suggest")
    public Result<String> suggest(@Valid @RequestBody AiSuggestDTO dto) {
        return Result.ok(aiService.suggest(dto));
    }

    @Operation(summary = "AI 对话（流式输出）", description = "对话式写作助手，支持 SSE 流式输出")
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@Valid @RequestBody AiChatDTO dto) {
        return aiService.chat(dto);
    }
}
