package com.blog.service;

/**
 * 评论 AI 自动审核服务
 */
public interface CommentModerationService {

    /**
     * 自动审核超时待处理的评论（由定时任务调用）
     */
    void autoReviewPendingComments();
}
