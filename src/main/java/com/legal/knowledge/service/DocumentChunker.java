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
        String normalized = normalizeForIndexing(text);

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

    /**
     * 文档切片前的文本规范化：
     * <ul>
     *     <li>统一各种空白符为空格</li>
     *     <li>合并连续空格</li>
     *     <li>将 3 行以上连续换行压缩为 2 行</li>
     *     <li>移除中文字符之间的空格（常见于 PDF/OCR 提取："中 华 人 民"）</li>
     * </ul>
     *
     * <p>目的：降低 embedding/BM25 的噪声，提升检索稳定性。</p>
     */
    private String normalizeForIndexing(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String normalized = text
                // 不间断空格（NBSP）常见于网页/PDF 提取
                .replace('\u00A0', ' ')
                // 将 tab/竖向 tab/换页/\r 统一成空格
                .replaceAll("[\\t\\x0B\\f\\r]+", " ")
                // 3 行以上连续空行压缩为 2 行，避免过多空白影响切片密度
                .replaceAll("\\n{3,}", "\n\n")
                // 合并连续空格（保留换行，方便段落语义）
                .replaceAll("[ ]{2,}", " ")
                .trim();

        // 移除中文字符之间的空格（仅影响中文，不破坏英文单词间空格）
        // 例："中 华 人 民 共 和 国" -> "中华人民共和国"
        normalized = normalized.replaceAll("(?<=[\\u4E00-\\u9FFF])[ ]+(?=[\\u4E00-\\u9FFF])", "");
        return normalized;
    }
}
