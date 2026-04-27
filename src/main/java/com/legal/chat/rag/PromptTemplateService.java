package com.legal.chat.rag;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class PromptTemplateService {

    private final String template;

    public PromptTemplateService(org.springframework.core.io.ResourceLoader resourceLoader) {
        Resource resource = resourceLoader.getResource("classpath:prompts/legal-rag-system.txt");
        try {
            this.template = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("无法加载 RAG 提示词模板", ex);
        }
    }

    public String renderSystemPrompt(String context, String knowledgeWarning) {
        return template
                .replace("{{context}}", context)
                .replace("{{knowledgeWarning}}", knowledgeWarning);
    }
}
