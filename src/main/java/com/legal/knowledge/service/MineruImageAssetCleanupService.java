package com.legal.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.legal.knowledge.entity.KbDocumentEntity;
import com.legal.knowledge.entity.KbDocumentImageAssetEntity;
import com.legal.knowledge.mapper.KbChunkImageRefMapper;
import com.legal.knowledge.mapper.KbDocumentMapper;
import com.legal.knowledge.mapper.KbDocumentImageAssetMapper;
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
    private final MineruImageAssetStorageService storageService;

    public MineruImageAssetCleanupService(KbDocumentImageAssetMapper imageAssetMapper,
                                          KbChunkImageRefMapper chunkImageRefMapper,
                                          KbDocumentMapper documentMapper,
                                          MineruImageAssetStorageService storageService) {
        this.imageAssetMapper = imageAssetMapper;
        this.chunkImageRefMapper = chunkImageRefMapper;
        this.documentMapper = documentMapper;
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
        try {
            storageService.deleteObjectsByPrefix(document.getDocumentOssBucket(), document.getDocumentOssPrefix());
        } catch (RuntimeException ex) {
            log.warn("删除软删除文档的 OSS 解析目录失败，继续完成本地软删除。tenantId={}, documentId={}, prefix={}, error={}",
                    tenantId, documentId, document.getDocumentOssPrefix(), ex.getMessage());
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
