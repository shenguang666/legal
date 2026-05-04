package com.legal.knowledge.service;

/**
 * MinerU 结果包中的图片条目。
 */
public record MineruImageEntry(
        String originalPath,
        String normalizedPath,
        byte[] bytes,
        String fileExt,
        String mimeType
) {
}
