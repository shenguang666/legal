package com.legal.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.legal.knowledge.entity.KbDocumentEntity;
import com.legal.knowledge.entity.KbDocumentImageAssetEntity;
import com.legal.knowledge.entity.KbDocumentParseTaskEntity;
import com.legal.knowledge.mapper.KbChunkImageRefMapper;
import com.legal.knowledge.mapper.KbDocumentMapper;
import com.legal.knowledge.mapper.KbDocumentImageAssetMapper;
import com.legal.knowledge.mapper.KbDocumentParseTaskMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class MineruImageAssetCleanupService {

    private static final Logger log = LoggerFactory.getLogger(MineruImageAssetCleanupService.class);

    private final KbDocumentImageAssetMapper imageAssetMapper;
    private final KbChunkImageRefMapper chunkImageRefMapper;
    private final KbDocumentMapper documentMapper;
    private final KbDocumentParseTaskMapper parseTaskMapper;
    private final MineruImageAssetStorageService storageService;

    public MineruImageAssetCleanupService(KbDocumentImageAssetMapper imageAssetMapper,
                                          KbChunkImageRefMapper chunkImageRefMapper,
                                          KbDocumentMapper documentMapper,
                                          KbDocumentParseTaskMapper parseTaskMapper,
                                          MineruImageAssetStorageService storageService) {
        this.imageAssetMapper = imageAssetMapper;
        this.chunkImageRefMapper = chunkImageRefMapper;
        this.documentMapper = documentMapper;
        this.parseTaskMapper = parseTaskMapper;
        this.storageService = storageService;
    }

    public void cleanupDeletedDocument(Long tenantId, Long documentId) {
        cleanupDocumentPackage(tenantId, documentId);
        List<KbDocumentImageAssetEntity> assets = imageAssetMapper.selectByDocument(tenantId, documentId);
        if (assets.isEmpty()) {
            return;
        }
        deleteOssObjects(tenantId, documentId, assets);
        chunkImageRefMapper.deleteByDocument(tenantId, documentId);
        imageAssetMapper.deleteByDocument(tenantId, documentId);
    }

    private void cleanupDocumentPackage(Long tenantId, Long documentId) {
        KbDocumentEntity document = documentMapper.selectOne(new LambdaQueryWrapper<KbDocumentEntity>()
                .eq(KbDocumentEntity::getTenantId, tenantId)
                .eq(KbDocumentEntity::getDocumentId, documentId)
                .last("limit 1"));
        if (document == null || !StringUtils.hasText(document.getDocumentOssPrefix())) {
            return;
        }
        int directDeleted = 0;
        try {
            directDeleted = deleteKnownPackageObjects(document);
        } catch (RuntimeException ex) {
            log.warn("删除软删除文档的 OSS 已知对象失败，继续尝试按前缀清理。tenantId={}, documentId={}, prefix={}, error={}",
                    tenantId, documentId, document.getDocumentOssPrefix(), ex.getMessage());
        }
        try {
            int prefixDeleted = storageService.deleteObjectsByPrefix(document.getDocumentOssBucket(), document.getDocumentOssPrefix());
            boolean parentDeleted = deleteParentDirectoryIfEmpty(document);
            log.info("删除软删除文档的 OSS 对象完成。tenantId={}, documentId={}, prefix={}, directDeleted={}, prefixDeleted={}, parentDeleted={}",
                    tenantId, documentId, document.getDocumentOssPrefix(), directDeleted, prefixDeleted, parentDeleted);
        } catch (RuntimeException ex) {
            log.warn("按 OSS 前缀列举清理文档目录失败，已完成已知对象删除并继续本地软删除。tenantId={}, documentId={}, prefix={}, directDeleted={}, error={}",
                    tenantId, documentId, document.getDocumentOssPrefix(), directDeleted, ex.getMessage());
        }
    }

    private boolean deleteParentDirectoryIfEmpty(KbDocumentEntity document) {
        String parentPrefix = parentDocumentPrefix(document.getDocumentOssPrefix());
        if (!StringUtils.hasText(parentPrefix)) {
            return false;
        }
        if (storageService.hasObjectsByPrefix(document.getDocumentOssBucket(), parentPrefix)) {
            return false;
        }
        boolean deletedMarker = storageService.deleteObject(document.getDocumentOssBucket(), parentPrefix);
        boolean deletedLegacyMarker = storageService.deleteObject(document.getDocumentOssBucket(), trimTrailingSlash(parentPrefix));
        return deletedMarker || deletedLegacyMarker;
    }

    private String parentDocumentPrefix(String versionPrefix) {
        if (!StringUtils.hasText(versionPrefix)) {
            return "";
        }
        String normalized = versionPrefix.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        int slash = normalized.lastIndexOf('/');
        if (slash < 0) {
            return "";
        }
        return normalized.substring(0, slash + 1);
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

    private int deleteKnownPackageObjects(KbDocumentEntity document) {
        Set<String> objectKeys = new LinkedHashSet<>();
        String prefix = document.getDocumentOssPrefix();
        objectKeys.add(prefix + "origin");
        objectKeys.add(prefix + "full.md");
        objectKeys.add(prefix + "content_list_v2.json");
        addOriginObjectKey(objectKeys, prefix, document.getSource());
        KbDocumentParseTaskEntity task = parseTaskMapper.selectOne(new LambdaQueryWrapper<KbDocumentParseTaskEntity>()
                .eq(KbDocumentParseTaskEntity::getTenantId, document.getTenantId())
                .eq(KbDocumentParseTaskEntity::getDocumentId, document.getDocumentId())
                .orderByDesc(KbDocumentParseTaskEntity::getTaskId)
                .last("limit 1"));
        if (task != null) {
            addOriginObjectKey(objectKeys, prefix, task.getFileName());
        }
        String existingOriginObject = storageService.findFirstObjectKeyByPrefix(document.getDocumentOssBucket(), prefix + "origin.");
        if (StringUtils.hasText(existingOriginObject)) {
            objectKeys.add(existingOriginObject);
        }
        int deletedCount = 0;
        for (String objectKey : objectKeys) {
            try {
                if (storageService.deleteObject(document.getDocumentOssBucket(), objectKey)) {
                    deletedCount++;
                }
            } catch (RuntimeException ex) {
                log.warn("删除文档 OSS 已知对象失败，继续删除其他对象。documentId={}, objectKey={}, error={}",
                        document.getDocumentId(), objectKey, ex.getMessage());
            }
        }
        return deletedCount;
    }

    private void addOriginObjectKey(Set<String> objectKeys, String prefix, String originalFileName) {
        String objectName = storageService.originObjectName(originalFileName);
        if (StringUtils.hasText(objectName)) {
            objectKeys.add(prefix + objectName);
        }
    }

    private void deleteOssObjects(Long tenantId, Long documentId, List<KbDocumentImageAssetEntity> assets) {
        Set<String> deletedKeys = new LinkedHashSet<>();
        for (KbDocumentImageAssetEntity asset : assets) {
            if (!StringUtils.hasText(asset.getOssObjectKey())) {
                continue;
            }
            String uniqueKey = (StringUtils.hasText(asset.getOssBucket()) ? asset.getOssBucket() : "") + ":" + asset.getOssObjectKey();
            if (!deletedKeys.add(uniqueKey)) {
                continue;
            }
            try {
                storageService.deleteObject(asset.getOssBucket(), asset.getOssObjectKey());
            } catch (RuntimeException ex) {
                log.warn("删除软删除文档的 OSS 图片对象失败，继续完成本地软删除。tenantId={}, documentId={}, objectKey={}, error={}",
                        tenantId, documentId, asset.getOssObjectKey(), ex.getMessage());
            }
        }
    }
}
