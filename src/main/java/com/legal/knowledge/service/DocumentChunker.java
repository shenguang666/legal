package com.legal.knowledge.service;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
public class DocumentChunker {

    private static final int CHUNK_SIZE = 700;
    private static final int CHUNK_OVERLAP = 120;

    public List<String> chunk(String text) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        String normalized = text.replaceAll("[\\t\\x0B\\f\\r]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(normalized.length(), start + CHUNK_SIZE);
            String part = normalized.substring(start, end).trim();
            if (StringUtils.hasText(part)) {
                chunks.add(part);
            }
            if (end >= normalized.length()) {
                break;
            }
            start = Math.max(end - CHUNK_OVERLAP, start + 1);
        }
        return chunks;
    }
}
