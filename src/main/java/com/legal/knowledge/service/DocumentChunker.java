package com.legal.knowledge.service;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
public class DocumentChunker {

    private static final int CHUNK_SIZE = 300;
    private static final int CHUNK_OVERLAP = 45;

    public List<String> chunk(String text) {
        return chunk(text, CHUNK_SIZE, CHUNK_OVERLAP);
    }

    public List<String> chunk(String text, int chunkSize, int chunkOverlap) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        String normalized = text.replaceAll("[\\t\\x0B\\f\\r]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();

        int safeChunkSize = Math.max(1, chunkSize);
        int safeOverlap = Math.max(0, Math.min(chunkOverlap, safeChunkSize - 1));
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(normalized.length(), start + safeChunkSize);
            String part = normalized.substring(start, end).trim();
            if (StringUtils.hasText(part)) {
                chunks.add(part);
            }
            if (end >= normalized.length()) {
                break;
            }
            start = Math.max(end - safeOverlap, start + 1);
        }
        return chunks;
    }
}
