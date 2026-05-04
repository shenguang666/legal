package com.legal.knowledge.service;

import com.aliyun.sdk.service.oss2.ClientConfiguration;
import com.aliyun.sdk.service.oss2.OSSClient;
import com.aliyun.sdk.service.oss2.PresignOptions;
import com.aliyun.sdk.service.oss2.credentials.StaticCredentialsProvider;
import com.aliyun.sdk.service.oss2.internal.ClientImpl;
import com.aliyun.sdk.service.oss2.models.DeleteObjectRequest;
import com.aliyun.sdk.service.oss2.models.GetObjectMetaRequest;
import com.aliyun.sdk.service.oss2.models.ListObjectsV2Request;
import com.aliyun.sdk.service.oss2.models.ListObjectsV2Result;
import com.aliyun.sdk.service.oss2.models.ObjectSummary;
import com.aliyun.sdk.service.oss2.models.GetObjectRequest;
import com.aliyun.sdk.service.oss2.models.PutObjectRequest;
import com.aliyun.sdk.service.oss2.models.PresignResult;
import com.aliyun.sdk.service.oss2.operations.Presigner;
import com.aliyun.sdk.service.oss2.transport.BinaryData;
import com.legal.common.AppException;
import com.legal.config.OssStorageProperties;
import com.legal.enums.KbDocumentBizType;
import com.legal.knowledge.entity.KbDocumentEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MineruImageAssetStorageService {

    private static final Pattern MARKDOWN_IMAGE_PATTERN = Pattern.compile("!\\[([^\\]]*)]\\(([^)]+)\\)");

    private final OssStorageProperties properties;

    public MineruImageAssetStorageService(OssStorageProperties properties) {
        this.properties = properties;
    }

    public boolean isEnabled() {
        return properties.isEnabled()
                && StringUtils.hasText(properties.getRegion())
                && StringUtils.hasText(properties.getBucket())
                && StringUtils.hasText(properties.getAccessKeyId())
                && StringUtils.hasText(properties.getAccessKeySecret());
    }

    public OssObjectReference upload(KbDocumentBizType bizType,
                                     String username,
                                     Long ownerUserId,
                                     Long tenantId,
                                     Long documentId,
                                     Integer docVersion,
                                     MineruImageEntry image) {
        if (!isEnabled()) {
            throw AppException.badRequest("OSS 未配置，无法上传 MinerU 图片资产");
        }
        String contentHash = sha256Hex(image.bytes());
        String objectKey = buildObjectKey(bizType, username, ownerUserId, tenantId, documentId, docVersion, contentHash, image.fileExt());
        try (OSSClient client = buildClient()) {
            client.putObject(PutObjectRequest.newBuilder()
                    .bucket(properties.getBucket())
                    .key(objectKey)
                    .body(BinaryData.fromBytes(image.bytes()))
                    .build());
        } catch (Exception ex) {
            throw AppException.badRequest("上传 MinerU 图片到 OSS 失败：" + ex.getMessage());
        }
        return new OssObjectReference(properties.getBucket(), objectKey, resolveUrl(objectKey));
    }

    public MineruPackageUploadResult uploadPackage(KbDocumentEntity document,
                                                   MineruParsePackage parsePackage,
                                                   String originalFileName,
                                                   byte[] originalFileContent) {
        if (!isEnabled()) {
            throw AppException.badRequest("OSS 未配置，无法上传 MinerU 解析结果包");
        }
        if (parsePackage == null || parsePackage.files().isEmpty()) {
            throw AppException.badRequest("MinerU 解析结果包为空，无法上传 OSS");
        }
        String objectPrefix = buildPackagePrefix(document);
        String markdownPath = parsePackage.markdownPath();
        String rewrittenMarkdown = rewriteMarkdownImageUrls(parsePackage.markdown(), markdownPath, objectPrefix);
        try (OSSClient client = buildClient()) {
            for (MineruPackageFile file : parsePackage.files()) {
                if (!StringUtils.hasText(file.normalizedPath())) {
                    continue;
                }
                byte[] fileBytes = file.bytes();
                if (file.normalizedPath().equals(markdownPath) && rewrittenMarkdown != null) {
                    fileBytes = rewrittenMarkdown.getBytes(StandardCharsets.UTF_8);
                }
                client.putObject(PutObjectRequest.newBuilder()
                        .bucket(properties.getBucket())
                        .key(objectPrefix + file.normalizedPath())
                        .contentType(file.mimeType())
                        .body(BinaryData.fromBytes(fileBytes))
                        .build());
                if (file.normalizedPath().equals(markdownPath) && !"full.md".equals(file.normalizedPath())) {
                    client.putObject(PutObjectRequest.newBuilder()
                            .bucket(properties.getBucket())
                            .key(objectPrefix + "full.md")
                            .contentType(file.mimeType())
                            .body(BinaryData.fromBytes(fileBytes))
                            .build());
                }
                if (file.normalizedPath().toLowerCase(Locale.ROOT).endsWith("content_list_v2.json")
                        && !"content_list_v2.json".equals(file.normalizedPath())) {
                    client.putObject(PutObjectRequest.newBuilder()
                            .bucket(properties.getBucket())
                            .key(objectPrefix + "content_list_v2.json")
                            .contentType(file.mimeType())
                            .body(BinaryData.fromBytes(fileBytes))
                            .build());
                }
            }
            if (originalFileContent != null && originalFileContent.length > 0) {
                client.putObject(PutObjectRequest.newBuilder()
                        .bucket(properties.getBucket())
                        .key(objectPrefix + originObjectName(originalFileName))
                        .contentType(mimeTypeByPath(originalFileName))
                        .contentDisposition("attachment; filename=\"" + safeDownloadName(originalFileName) + "\"")
                        .body(BinaryData.fromBytes(originalFileContent))
                        .build());
            }
        } catch (Exception ex) {
            throw AppException.badRequest("上传 MinerU 解析结果包到 OSS 失败：" + ex.getMessage());
        }
        return new MineruPackageUploadResult(properties.getBucket(), objectPrefix, resolveDirectoryUrl(objectPrefix), rewrittenMarkdown);
    }

    public void deleteObject(String bucket, String objectKey) {
        if (!isEnabled()) {
            return;
        }
        if (!StringUtils.hasText(objectKey)) {
            return;
        }
        String targetBucket = StringUtils.hasText(bucket) ? bucket : properties.getBucket();
        try (OSSClient client = buildClient()) {
            client.deleteObject(DeleteObjectRequest.newBuilder()
                    .bucket(targetBucket)
                    .key(objectKey)
                    .build());
        } catch (Exception ex) {
            throw AppException.badRequest("删除 OSS 图片对象失败：" + ex.getMessage());
        }
    }

    public void deleteObjectsByPrefix(String bucket, String objectPrefix) {
        if (!isEnabled()) {
            return;
        }
        if (!StringUtils.hasText(objectPrefix)) {
            return;
        }
        String targetBucket = StringUtils.hasText(bucket) ? bucket : properties.getBucket();
        try (OSSClient client = buildClient()) {
            String continuationToken = null;
            do {
                ListObjectsV2Result result = client.listObjectsV2(ListObjectsV2Request.newBuilder()
                        .bucket(targetBucket)
                        .prefix(objectPrefix)
                        .maxKeys(1000L)
                        .continuationToken(continuationToken)
                        .build());
                if (result.contents() != null) {
                    for (ObjectSummary object : result.contents()) {
                        if (StringUtils.hasText(object.key())) {
                            client.deleteObject(DeleteObjectRequest.newBuilder()
                                    .bucket(targetBucket)
                                    .key(object.key())
                                    .build());
                        }
                    }
                }
                continuationToken = Boolean.TRUE.equals(result.isTruncated()) ? result.nextContinuationToken() : null;
            } while (StringUtils.hasText(continuationToken));
        } catch (Exception ex) {
            throw AppException.badRequest("删除 OSS 文档目录失败：" + ex.getMessage());
        }
    }

    public boolean objectExists(String bucket, String objectKey) {
        if (!isEnabled() || !StringUtils.hasText(objectKey)) {
            return false;
        }
        String targetBucket = StringUtils.hasText(bucket) ? bucket : properties.getBucket();
        try (OSSClient client = buildClient()) {
            return client.doesObjectExist(GetObjectMetaRequest.newBuilder()
                    .bucket(targetBucket)
                    .key(objectKey)
                    .build());
        } catch (Exception ex) {
            return false;
        }
    }

    public String buildObjectKey(KbDocumentBizType bizType,
                                 String username,
                                 Long ownerUserId,
                                 Long tenantId,
                                 Long documentId,
                                 Integer docVersion,
                                 String contentHash,
                                 String fileExt) {
        String category = switch (bizType == null ? KbDocumentBizType.KNOWLEDGE : bizType) {
            case KNOWLEDGE -> "knowledge";
            case TIANYAN_REVIEW -> "tianyan-review";
            case RISK_RULE -> "risk-rule";
        };
        String userSegment = sanitizeSegment(username) + "-" + ownerUserId;
        String ext = StringUtils.hasText(fileExt) ? fileExt.toLowerCase(Locale.ROOT).replace(".", "") : "bin";
        return "mineru-assets/" + category
                + "/" + userSegment
                + "/tenant-" + tenantId
                + "/doc-" + documentId
                + "/v" + docVersion
                + "/images/" + contentHash + "." + ext;
    }

    public String buildPackagePrefix(KbDocumentEntity document) {
        KbDocumentBizType bizType = document == null ? null : document.getBizType();
        String category = switch (bizType == null ? KbDocumentBizType.KNOWLEDGE : bizType) {
            case KNOWLEDGE -> "knowledge";
            case TIANYAN_REVIEW -> "tianyan-review";
            case RISK_RULE -> "risk-rule";
        };
        return "mineru-documents/" + category
                + "/tenant-" + (document == null ? "unknown" : document.getTenantId())
                + "/doc-" + (document == null ? "unknown" : document.getDocumentId())
                + "/v" + (document == null ? "unknown" : document.getDocVersion())
                + "/";
    }

    public String resolveUrl(String objectKey) {
        return resolveUrl(properties.getBucket(), objectKey);
    }

    public String resolveUrl(String bucket, String objectKey) {
        return resolveUrl(bucket, objectKey, null);
    }

    public String resolveUrl(String bucket, String objectKey, String contentDisposition) {
        if (StringUtils.hasText(properties.getPublicBaseUrl())) {
            return trimTrailingSlash(properties.getPublicBaseUrl()) + "/" + objectKey;
        }
        String targetBucket = StringUtils.hasText(bucket) ? bucket : properties.getBucket();
        ClientConfiguration config = ClientConfiguration.newBuilder()
                .credentialsProvider(credentialsProvider())
                .region(properties.getRegion())
                .build();
        try (ClientImpl client = new ClientImpl(config)) {
            GetObjectRequest.Builder requestBuilder = GetObjectRequest.newBuilder()
                    .bucket(targetBucket)
                    .key(objectKey);
            if (StringUtils.hasText(contentDisposition)) {
                requestBuilder.responseContentDisposition(contentDisposition);
            }
            GetObjectRequest request = requestBuilder.build();
            PresignOptions options = PresignOptions.newBuilder()
                    .expiration(properties.getPresignedUrlExpiration())
                    .build();
            PresignResult result = Presigner.getObject(client, request, options);
            return result.url();
        } catch (Exception ex) {
            throw AppException.badRequest("生成 OSS 图片访问地址失败：" + ex.getMessage());
        }
    }

    public String resolveDirectoryUrl(String objectPrefix) {
        if (StringUtils.hasText(properties.getPublicBaseUrl())) {
            return trimTrailingSlash(properties.getPublicBaseUrl()) + "/" + objectPrefix;
        }
        String endpointHost = StringUtils.hasText(properties.getEndpoint())
                ? trimTrailingSlash(properties.getEndpoint()).replaceFirst("^https?://", "")
                : properties.getBucket() + ".oss-" + properties.getRegion() + ".aliyuncs.com";
        String host = endpointHost.startsWith(properties.getBucket() + ".") ? endpointHost : properties.getBucket() + "." + endpointHost;
        return "https://" + host + "/" + objectPrefix;
    }

    private String rewriteMarkdownImageUrls(String markdown, String markdownPath, String objectPrefix) {
        if (!StringUtils.hasText(markdown)) {
            return markdown;
        }
        Matcher matcher = MARKDOWN_IMAGE_PATTERN.matcher(markdown);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String imagePath = cleanMarkdownLinkTarget(matcher.group(2));
            if (isExternalUrl(imagePath)) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group()));
                continue;
            }
            String normalizedPath = resolveRelativePackagePath(markdownPath, imagePath);
            if (!StringUtils.hasText(normalizedPath)) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group()));
                continue;
            }
            String replacement = "![" + matcher.group(1) + "](" + resolveDirectoryUrl(objectPrefix) + normalizedPath + ")";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private String resolveRelativePackagePath(String markdownPath, String imagePath) {
        String normalizedImagePath = MineruClient.normalizeZipPath(imagePath);
        if (!StringUtils.hasText(normalizedImagePath)) {
            return null;
        }
        String markdownDir = "";
        if (StringUtils.hasText(markdownPath) && markdownPath.contains("/")) {
            markdownDir = markdownPath.substring(0, markdownPath.lastIndexOf('/') + 1);
        }
        return MineruClient.normalizeZipPath(markdownDir + normalizedImagePath);
    }

    private String cleanMarkdownLinkTarget(String target) {
        String value = target == null ? "" : target.trim();
        if ((value.startsWith("<") && value.endsWith(">")) || (value.startsWith("\"") && value.endsWith("\""))) {
            value = value.substring(1, value.length() - 1).trim();
        }
        int anchorIndex = value.indexOf('#');
        if (anchorIndex >= 0) {
            value = value.substring(0, anchorIndex);
        }
        int queryIndex = value.indexOf('?');
        if (queryIndex >= 0) {
            value = value.substring(0, queryIndex);
        }
        return value;
    }

    private boolean isExternalUrl(String value) {
        String lower = value == null ? "" : value.toLowerCase(Locale.ROOT);
        return lower.startsWith("http://")
                || lower.startsWith("https://")
                || lower.startsWith("data:")
                || lower.startsWith("mailto:");
    }

    private String mimeTypeByPath(String path) {
        String value = path == null ? "" : path.toLowerCase(Locale.ROOT);
        if (value.endsWith(".pdf")) {
            return "application/pdf";
        }
        if (value.endsWith(".doc")) {
            return "application/msword";
        }
        if (value.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }
        if (value.endsWith(".md")) {
            return "text/markdown; charset=utf-8";
        }
        if (value.endsWith(".txt")) {
            return "text/plain; charset=utf-8";
        }
        return "application/octet-stream";
    }

    private String safeDownloadName(String originalFileName) {
        if (!StringUtils.hasText(originalFileName)) {
            return "origin";
        }
        return originalFileName.replace("\\", "_").replace("\"", "_");
    }

    public String originObjectName(String originalFileName) {
        String ext = originalFileExtension(originalFileName);
        return StringUtils.hasText(ext) ? "origin" + ext : "origin";
    }

    private String originalFileExtension(String originalFileName) {
        if (!StringUtils.hasText(originalFileName)) {
            return "";
        }
        String name = originalFileName.replace("\\", "/");
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        String ext = name.substring(dot).toLowerCase(Locale.ROOT);
        return ext.matches("\\.[a-z0-9]{1,15}") ? ext : "";
    }

    public String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes == null ? new byte[0] : bytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("当前运行环境不支持 SHA-256", ex);
        }
    }

    private OSSClient buildClient() {
        var builder = OSSClient.newBuilder()
                .credentialsProvider(credentialsProvider())
                .region(properties.getRegion());
        if (StringUtils.hasText(properties.getEndpoint())) {
            builder.endpoint(properties.getEndpoint());
        }
        return builder.build();
    }

    private StaticCredentialsProvider credentialsProvider() {
        return new StaticCredentialsProvider(properties.getAccessKeyId(), properties.getAccessKeySecret());
    }

    private String sanitizeSegment(String value) {
        String text = StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : "user";
        String sanitized = text.replaceAll("[^a-z0-9._-]", "-").replaceAll("-+", "-");
        return StringUtils.hasText(sanitized) ? sanitized : "user";
    }

    private String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String result = value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
