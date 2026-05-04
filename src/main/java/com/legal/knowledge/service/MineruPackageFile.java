package com.legal.knowledge.service;

/**
 * MinerU 解析结果包内的单个解压文件。
 */
public record MineruPackageFile(
        String originalPath,
        String normalizedPath,
        byte[] bytes,
        String mimeType
) {

    public MineruPackageFile {
        bytes = bytes == null ? new byte[0] : bytes.clone();
    }

    @Override
    public byte[] bytes() {
        return bytes.clone();
    }
}
