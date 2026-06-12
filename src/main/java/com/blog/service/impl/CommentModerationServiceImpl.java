package com.blog.service.impl;

import com.blog.entity.Comment;
import com.blog.mapper.CommentMapper;
import com.blog.service.CommentModerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentModerationServiceImpl implements CommentModerationService {

    private final CommentMapper commentMapper;
    private final ChatClient chatClient;

    /** 超过此秒数仍未人工审核的评论，触发 AI 自动审核 */
    private static final int PENDING_THRESHOLD_SECONDS = 30;

    private static final String AI_REVIEW_PROMPT = """
            你是一个严格的内容审核助手。请判断以下评论是否包含脏话、恶俗语言、人身攻击、违法内容或垃圾广告。
            请只回答 PASS 或 REJECT，不要解释原因。

            评论内容：
            %s
            """;

    @Override
    @Scheduled(fixedDelay = 30 * 1000, initialDelay = 30 * 1000)
    @Transactional
    public void autoReviewPendingComments() {
        List<Comment> pending = commentMapper.findPendingOlderThanSeconds(PENDING_THRESHOLD_SECONDS);
        if (pending.isEmpty()) {
            return;
        }
        log.info("[AI自动审核] 发现 {} 条待审核评论，开始处理...", pending.size());
        int approved = 0, rejected = 0;
        for (Comment comment : pending) {
            try {
                boolean passed = doAiModeration(comment.getContent());
                commentMapper.updateStatus(comment.getId(), passed ? 1 : 2);
                if (passed) {
                    approved++;
                } else {
                    rejected++;
                }
            } catch (Exception e) {
                log.error("[AI自动审核] 评论 ID={} 审核失败", comment.getId(), e);
            }
        }
        log.info("[AI自动审核] 完成：通过 {} 条，拒绝 {} 条", approved, rejected);
    }

    private boolean doAiModeration(String content) {
        try {
            String result = CompletableFuture.supplyAsync(() ->
                    chatClient.prompt()
                            .system("你是一个严格的内容审核助手，只回答PASS或REJECT。")
                            .user(String.format(AI_REVIEW_PROMPT, content))
                            .call()
                            .content()
            ).orTimeout(30, TimeUnit.SECONDS).join();
            return result != null && result.trim().toUpperCase().startsWith("PASS");
        } catch (Exception e) {
            log.error("AI 评论审核调用异常，默认通过", e);
            return true;
        }
    }
}
