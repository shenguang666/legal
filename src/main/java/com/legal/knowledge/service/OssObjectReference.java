package com.legal.knowledge.service;

/**
 * OSS 对象上传结果。
 */
public record OssObjectReference(
        String bucket,
        String objectKey,
        String publicUrl
) {
}
