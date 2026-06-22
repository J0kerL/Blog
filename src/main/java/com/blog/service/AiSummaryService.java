package com.blog.service;

/**
 * AI 摘要生成服务接口
 *
 * <p>负责调用 AI 服务生成文章摘要，支持容错处理。</p>
 *
 * @author Diamond
 * @since 1.0.0
 */
public interface AiSummaryService {

    /**
     * 根据文章标题和内容生成摘要
     *
     * <p>调用 AI 服务生成 120 字以内的中文摘要。如果 AI 调用失败，返回空字符串。</p>
     *
     * @param title   文章标题
     * @param content 文章内容（Markdown 格式）
     * @return 生成的摘要，失败时返回空字符串
     */
    String generateSummary(String title, String content);
}
