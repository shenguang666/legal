package com.legal.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 阿里云 OSS 文档资产存储配置。
 */
@Data
@ConfigurationProperties(prefix = "legal.storage.oss")
public class OssStorageProperties {

    /** 是否启用阿里云 OSS 文档资产上传能力。 */
    private boolean enabled = false;

    /** OSS 区域标识，例如 cn-hangzhou。 */
    private String region;

    /** OSS 访问端点，例如 https://oss-cn-hangzhou.aliyuncs.com。 */
    private String endpoint;

    /** OSS Bucket 名称。 */
    private String bucket;

    /** 项目自定义 OSS AccessKey ID。 */
    private String accessKeyId;

    /** 项目自定义 OSS AccessKey Secret。 */
    private String accessKeySecret;

    /** OSS 公开访问域名，为空时通过后端生成私有签名 URL。 */
    private String publicBaseUrl;

    /** 私有 Bucket 图片访问签名 URL 有效期。 */
    private Duration presignedUrlExpiration = Duration.ofHours(1);
}
