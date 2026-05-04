package com.legal.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.legal.common.AppException;
import com.legal.enums.KbDocumentStatus;
import com.legal.knowledge.entity.KbDocumentEntity;
import com.legal.knowledge.entity.KbDocumentParseTaskEntity;
import com.legal.knowledge.mapper.KbDocumentMapper;
import com.legal.knowledge.mapper.KbDocumentParseTaskMapper;
import com.legal.security.AuthPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Service
public class DocumentAssetService {

    private static final Set<String> ALLOWED_ASSETS = Set.of("origin", "full.md", "content_list_v2.json");

    private final KbDocumentMapper documentMapper;
    private final KbDocumentParseTaskMapper parseTaskMapper;
    private final MineruImageAssetStorageService storageService;

    public DocumentAssetService(KbDocumentMapper documentMapper,
                                KbDocumentParseTaskMapper parseTaskMapper,
                                MineruImageAssetStorageService storageService) {
        this.documentMapper = documentMapper;
        this.parseTaskMapper = parseTaskMapper;
        this.storageService = storageService;
    }

    public String createAccessUrl(AuthPrincipal principal, Long documentId, String assetName) {
        if (!ALLOWED_ASSETS.contains(assetName)) {
            throw AppException.badRequest("不支持访问该文档资产");
        }
        KbDocumentEntity document = documentMapper.selectOne(new LambdaQueryWrapper<KbDocumentEntity>()
                .eq(KbDocumentEntity::getTenantId, principal.tenantId())
                .eq(KbDocumentEntity::getOwnerUserId, principal.userId())
                .eq(KbDocumentEntity::getDocumentId, documentId)
                .last("limit 1"));
        if (document == null || document.getStatus() == KbDocumentStatus.DELETED) {
            throw AppException.notFound("文档不存在");
        }
        if (!StringUtils.hasText(document.getDocumentOssPrefix())) {
            throw AppException.badRequest("当前文档暂未记录 OSS 解析目录");
        }
        String downloadName = downloadName(document, assetName);
        String objectKey = objectKey(document, assetName, downloadName);
        return storageService.resolveUrl(document.getDocumentOssBucket(), objectKey, contentDisposition(downloadName));
    }

    private String objectKey(KbDocumentEntity document, String assetName, String downloadName) {
        if (!"origin".equals(assetName)) {
            return document.getDocumentOssPrefix() + assetName;
        }
        String objectKey = document.getDocumentOssPrefix() + storageService.originObjectName(downloadName);
        if (storageService.objectExists(document.getDocumentOssBucket(), objectKey)) {
            return objectKey;
        }
        return document.getDocumentOssPrefix() + "origin";
    }

    private String downloadName(KbDocumentEntity document, String assetName) {
        if (!"origin".equals(assetName)) {
            return assetName;
        }
        KbDocumentParseTaskEntity task = parseTaskMapper.selectOne(new LambdaQueryWrapper<KbDocumentParseTaskEntity>()
                .eq(KbDocumentParseTaskEntity::getTenantId, document.getTenantId())
                .eq(KbDocumentParseTaskEntity::getDocumentId, document.getDocumentId())
                .orderByDesc(KbDocumentParseTaskEntity::getTaskId)
                .last("limit 1"));
        if (task != null && StringUtils.hasText(task.getFileName())) {
            return safeDownloadName(task.getFileName());
        }
        if (StringUtils.hasText(document.getSource())) {
            return safeDownloadName(document.getSource());
        }
        return "origin";
    }

    private String safeDownloadName(String fileName) {
        return fileName.replace("\\", "_").replace("\"", "_").replace("/", "_");
    }

    private String contentDisposition(String fileName) {
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + encoded;
    }
}
