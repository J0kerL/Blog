package com.blog.service.impl;

import com.blog.common.BusinessException;
import com.blog.common.ResultCode;
import com.blog.dto.AiChatDTO;
import com.blog.dto.AiGenerateDTO;
import com.blog.dto.AiPolishDTO;
import com.blog.dto.AiSuggestDTO;
import com.blog.service.AiService;
import com.blog.util.RateLimitUtil;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final ChatClient chatClient;
    private final StringRedisTemplate redisTemplate;
    private final RateLimitUtil rateLimitUtil;

    private static final String AI_SESSION_PREFIX = "ai:session:";
    private static final String AI_RATE_PREFIX = "rate:ai:";
    private static final Duration SESSION_TTL = Duration.ofMinutes(30);
    private static final Duration AI_CALL_TIMEOUT = Duration.ofSeconds(60);
    private static final Duration RATE_WINDOW = Duration.ofMinutes(1);
    private static final long MAX_REQUESTS_PER_MINUTE = 20;

    @Override
    public String generateArticle(AiGenerateDTO dto) {
        checkAiRateLimit();
        if (dto.getPrompt() != null && dto.getPrompt().length() > 2000) {
            throw new BusinessException(400, "生成提示词不能超过 2000 个字符");
        }
        if (dto.getTitle() != null && dto.getTitle().length() > 200) {
            throw new BusinessException(400, "标题不能超过 200 个字符");
        }

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
            return CompletableFuture.supplyAsync(() ->
                    chatClient.prompt()
                            .user(prompt)
                            .call()
                            .content()
            ).orTimeout(AI_CALL_TIMEOUT.toSeconds(), TimeUnit.SECONDS).join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof TimeoutException) {
                log.error("AI 生成文章超时");
                throw new BusinessException(ResultCode.AI_REQUEST_FAILED);
            }
            log.error("AI 生成文章失败", e);
            throw new BusinessException(ResultCode.AI_REQUEST_FAILED);
        }
    }

    @Override
    public String polishArticle(AiPolishDTO dto) {
        checkAiRateLimit();
        if (dto.getContent() != null && dto.getContent().length() > 50000) {
            throw new BusinessException(400, "文章内容不能超过 50000 个字符");
        }
        if (dto.getInstruction() != null && dto.getInstruction().length() > 500) {
            throw new BusinessException(400, "润色指令不能超过 500 个字符");
        }

        String instruction = dto.getInstruction() != null ? dto.getInstruction() : "优化语言表达，使其更加专业流畅";

        String prompt = String.format("""
                请润色以下 Markdown 文章内容。要求：%s

                保持 Markdown 格式不变，只优化文字内容。

                原文：
                %s
                """, instruction, dto.getContent());

        try {
            return CompletableFuture.supplyAsync(() ->
                    chatClient.prompt()
                            .user(prompt)
                            .call()
                            .content()
            ).orTimeout(AI_CALL_TIMEOUT.toSeconds(), TimeUnit.SECONDS).join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof TimeoutException) {
                log.error("AI 润色超时");
                throw new BusinessException(ResultCode.AI_REQUEST_FAILED);
            }
            log.error("AI 润色失败", e);
            throw new BusinessException(ResultCode.AI_REQUEST_FAILED);
        }
    }

    @Override
    public String suggest(AiSuggestDTO dto) {
        checkAiRateLimit();
        if (dto.getTitle() != null && dto.getTitle().length() > 200) {
            throw new BusinessException(400, "标题不能超过 200 个字符");
        }
        if (dto.getContent() != null && dto.getContent().length() > 50000) {
            throw new BusinessException(400, "文章内容不能超过 50000 个字符");
        }

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
            return CompletableFuture.supplyAsync(() ->
                    chatClient.prompt()
                            .user(prompt)
                            .call()
                            .content()
            ).orTimeout(AI_CALL_TIMEOUT.toSeconds(), TimeUnit.SECONDS).join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof TimeoutException) {
                log.error("AI 建议生成超时");
                throw new BusinessException(ResultCode.AI_REQUEST_FAILED);
            }
            log.error("AI 建议生成失败", e);
            throw new BusinessException(ResultCode.AI_REQUEST_FAILED);
        }
    }

    @Override
    public Flux<String> chat(AiChatDTO dto) {
        checkAiRateLimit();
        if (dto.getMessage() != null && dto.getMessage().length() > 2000) {
            throw new BusinessException(400, "消息内容不能超过 2000 个字符");
        }

        String sessionId = dto.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = "session-" + System.currentTimeMillis();
        }

        String redisKey = AI_SESSION_PREFIX + sessionId;

        // 从 Redis 获取历史对话
        List<String> rawHistory = redisTemplate.opsForList().range(redisKey, 0, -1);
        if (rawHistory == null) {
            rawHistory = new ArrayList<>();
        }

        // 限制历史记录为最近 20 条
        if (rawHistory.size() > 20) {
            rawHistory = new ArrayList<>(rawHistory.subList(rawHistory.size() - 20, rawHistory.size()));
        }

        // 将用户消息加入历史（使用 mutable list 供 lambda 使用）
        final List<String> history = new ArrayList<>(rawHistory);
        history.add("User: " + dto.getMessage());

        // 构建上下文 prompt
        StringBuilder contextPrompt = new StringBuilder();
        contextPrompt.append("以下是对话历史：\n");
        for (String msg : history) {
            contextPrompt.append(msg).append("\n");
        }
        contextPrompt.append("\n请回复用户最新的消息。");

        StringBuilder responseCollector = new StringBuilder();

        return chatClient.prompt()
                .user(contextPrompt.toString())
                .stream()
                .content()
                .doOnNext(chunk -> responseCollector.append(chunk))
                .doOnComplete(() -> {
                    // 对话完成后将 AI 实际回复加入历史
                    String assistantReply = responseCollector.toString();
                    if (!assistantReply.isBlank()) {
                        history.add("Assistant: " + assistantReply);
                    }
                    // 存储前再次裁剪历史，保留最近 20 条
                    List<String> toStore = history.size() > 20
                            ? new ArrayList<>(history.subList(history.size() - 20, history.size()))
                            : history;
                    redisTemplate.delete(redisKey);
                    if (!toStore.isEmpty()) {
                        redisTemplate.opsForList().rightPushAll(redisKey, toStore);
                    }
                    redisTemplate.expire(redisKey, SESSION_TTL);
                })
                .doOnError(e -> log.error("AI 对话流式输出异常", e));
    }

    private void checkAiRateLimit() {
        Long userId = StpUtil.getLoginIdAsLong();
        rateLimitUtil.checkRateLimit(AI_RATE_PREFIX + userId, MAX_REQUESTS_PER_MINUTE, RATE_WINDOW);
    }
}
