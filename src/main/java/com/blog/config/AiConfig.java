package com.blog.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    public static final String SYSTEM_PROMPT = """
            你是一个专业的博客写作助手。你的职责是：
            1. 帮助用户生成高质量的博客文章（Markdown 格式）
            2. 润色和优化现有文章内容
            3. 建议合适的标签、摘要和 SEO 描述
            4. 提供写作建议和创意灵感

            请始终使用 Markdown 格式输出文章内容，确保内容专业、有深度、可读性强。
            """;

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }
}
