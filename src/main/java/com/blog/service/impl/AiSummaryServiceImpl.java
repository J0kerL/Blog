package com.blog.service.impl;

import com.blog.service.AiSummaryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;

/**
 * AI 摘要生成服务实现类
 *
 * <p>调用 AI 服务生成文章摘要，支持超时控制和容错处理。</p>
 *
 * @author Diamond
 * @since 1.0.0
 */
@Slf4j
@Service
public class AiSummaryServiceImpl implements AiSummaryService {

    private final ChatClient chatClient;
    private final Executor aiExecutor;

    /** AI 调用超时时间（秒） */
    private static final long AI_CALL_TIMEOUT_SECONDS = 30;

    public AiSummaryServiceImpl(ChatClient chatClient,
                                @Qualifier("aiExecutor") Executor aiExecutor) {
        this.chatClient = chatClient;
        this.aiExecutor = aiExecutor;
    }

    /**
     * 根据文章标题和内容生成摘要
     */
    @Override
    public String generateSummary(String title, String content) {
        try {
            String truncatedContent = content.length() > 1000
                    ? content.substring(0, 1000)
                    : content;

            String prompt = String.format("""
                    请根据以下博客文章信息，生成一段 120 字以内的中文摘要，简洁概括文章核心内容，不要包含开头"本文"字样：

                    标题：%s
                    内容片段：%s
                    """,
                    title != null ? title : "未提供",
                    truncatedContent
            );

            return CompletableFuture.supplyAsync(() ->
                    chatClient.prompt()
                            .user(prompt)
                            .call()
                            .content()
            , aiExecutor).orTimeout(AI_CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS).join();

        } catch (CompletionException e) {
            if (e.getCause() instanceof TimeoutException) {
                log.warn("AI 生成摘要超时，文章标题: {}", title);
            } else {
                log.warn("AI 生成摘要失败，文章标题: {}, 错误: {}", title, e.getMessage());
            }
            return "";
        } catch (Exception e) {
            log.warn("AI 生成摘要异常，文章标题: {}, 错误: {}", title, e.getMessage());
            return "";
        }
    }
}
