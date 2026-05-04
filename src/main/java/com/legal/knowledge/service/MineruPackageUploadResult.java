package com.legal.knowledge.service;

/**
 * MinerU 解析结果包上传 OSS 后的访问信息。
 */
public record MineruPackageUploadResult(
        String bucket,
        String objectPrefix,
        String documentUrl,
        String markdown
) {
}
