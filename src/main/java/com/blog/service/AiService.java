package com.blog.service;

import com.blog.dto.AiChatDTO;
import com.blog.dto.AiGenerateDTO;
import com.blog.dto.AiPolishDTO;
import com.blog.dto.AiSuggestDTO;
import reactor.core.publisher.Flux;

public interface AiService {

    String generateArticle(AiGenerateDTO dto);

    String polishArticle(AiPolishDTO dto);

    String suggest(AiSuggestDTO dto);

    Flux<String> chat(AiChatDTO dto);
}
