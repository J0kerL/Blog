package com.blog.service.impl;

import com.blog.common.BusinessException;
import com.blog.dto.AiChatDTO;
import com.blog.dto.AiGenerateDTO;
import com.blog.dto.AiPolishDTO;
import com.blog.dto.AiSuggestDTO;
import com.blog.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final ChatClient chatClient;
    private final StringRedisTemplate redisTemplate;

    private static final String AI_SESSION_PREFIX = "ai:session:";
    private static final Duration SESSION_TTL = Duration.ofMinutes(30);

    @Override
    public String generateArticle(AiGenerateDTO dto) {
        String lengthDesc = switch (dto.getLength() != null ? dto.getLength() : "medium") {
            case "short" -> "800-1500字";
            case "long" -> "3000-5000字";
            default -> "1500-3000字";
        };
        String styleDesc = switch (dto.getStyle() != null ? dto.getStyle() : "technical") {
            case "casual" -> "轻松随意、口语化";
            case "academic" -> "学术严谨、引用规范";
            default -> "技术专业、逻辑清晰";
        };

        String prompt = String.format("""
                请根据以下要求生成一篇完整的博客文章，使用 Markdown 格式输出：

                主题：%s
                %s%s
                字数要求：%s
                写作风格：%s

                要求：
                1. 包含清晰的标题层级（H1/H2/H3）
                2. 包含代码示例（如适用）
                3. 包含总结段落
                4. 内容原创、有深度
                """,
                dto.getPrompt(),
                dto.getTitle() != null ? "指定标题：" + dto.getTitle() + "\n" : "",
                "",
                lengthDesc,
                styleDesc
        );

        try {
            return chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("AI 生成文章失败", e);
            throw new BusinessException(1009, "AI 服务请求失败：" + e.getMessage());
        }
    }

    @Override
    public String polishArticle(AiPolishDTO dto) {
        String instruction = dto.getInstruction() != null ? dto.getInstruction() : "优化语言表达，使其更加专业流畅";

        String prompt = String.format("""
                请润色以下 Markdown 文章内容。要求：%s

                保持 Markdown 格式不变，只优化文字内容。

                原文：
                %s
                """, instruction, dto.getContent());

        try {
            return chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("AI 润色失败", e);
            throw new BusinessException(1009, "AI 服务请求失败：" + e.getMessage());
        }
    }

    @Override
    public String suggest(AiSuggestDTO dto) {
        String prompt = String.format("""
                请根据以下博客文章信息，生成：
                1. 3-5个合适的标签
                2. 一段 150 字以内的 SEO 摘要
                3. 一段 SEO description（英文，160 字符以内）

                标题：%s
                %s

                请使用 JSON 格式返回：{"tags": [...], "summary": "...", "seoDescription": "..."}
                """,
                dto.getTitle() != null ? dto.getTitle() : "未提供",
                dto.getContent() != null ? "内容摘要：" + dto.getContent().substring(0, Math.min(500, dto.getContent().length())) : ""
        );

        try {
            return chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("AI 建议生成失败", e);
            throw new BusinessException(1009, "AI 服务请求失败：" + e.getMessage());
        }
    }

    @Override
    public Flux<String> chat(AiChatDTO dto) {
        String sessionId = dto.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = "session-" + System.currentTimeMillis();
        }

        String redisKey = AI_SESSION_PREFIX + sessionId;

        // 从 Redis 获取历史对话
        List<String> history = redisTemplate.opsForList().range(redisKey, 0, -1);
        if (history == null) {
            history = new ArrayList<>();
        }

        // 将用户消息加入历史
        history.add("User: " + dto.getMessage());

        // 构建上下文 prompt
        StringBuilder contextPrompt = new StringBuilder();
        contextPrompt.append("以下是对话历史：\n");
        for (String msg : history) {
            contextPrompt.append(msg).append("\n");
        }
        contextPrompt.append("\n请回复用户最新的消息。");

        return chatClient.prompt()
                .user(contextPrompt.toString())
                .stream()
                .content()
                .doOnComplete(() -> {
                    // 对话完成后将 AI 回复也加入历史（简化存储）
                    // 实际生产中可以存储完整回复
                    redisTemplate.opsForList().rightPush(redisKey, "Assistant: [回复已生成]");
                    redisTemplate.expire(redisKey, SESSION_TTL);
                })
                .doOnError(e -> log.error("AI 对话流式输出异常", e));
    }
}
