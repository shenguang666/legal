package com.legal.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.legal.auth.entity.LegalUserEntity;
import com.legal.auth.mapper.LegalUserMapper;
import com.legal.config.DocumentProcessingProperties;
import com.legal.enums.ImageCaptionStatus;
import com.legal.knowledge.entity.KbDocumentEntity;
import com.legal.knowledge.entity.KbDocumentImageAssetEntity;
import com.legal.knowledge.mapper.KbDocumentImageAssetMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MineruImageAssetProcessor {

    private static final Logger log = LoggerFactory.getLogger(MineruImageAssetProcessor.class);
    private static final Pattern MARKDOWN_IMAGE_PATTERN = Pattern.compile("!\\[([^]]*)]\\(([^)]+)\\)");

    private final DocumentProcessingProperties properties;
    private final LegalUserMapper legalUserMapper;
    private final KbDocumentImageAssetMapper imageAssetMapper;
    private final MineruImageAssetStorageService storageService;
    private final MineruImageCaptionService captionService;

    public MineruImageAssetProcessor(DocumentProcessingProperties properties,
                                     LegalUserMapper legalUserMapper,
                                     KbDocumentImageAssetMapper imageAssetMapper,
                                     MineruImageAssetStorageService storageService,
                                     MineruImageCaptionService captionService) {
        this.properties = properties;
        this.legalUserMapper = legalUserMapper;
        this.imageAssetMapper = imageAssetMapper;
        this.storageService = storageService;
        this.captionService = captionService;
    }

    public MineruImageProcessingResult process(KbDocumentEntity document, MineruParsePackage parsePackage) {
        if (parsePackage == null || !StringUtils.hasText(parsePackage.markdown())) {
            return new MineruImageProcessingResult(null, List.of());
        }
        if (!properties.getMineru().getImageAsset().isEnabled() || parsePackage.images().isEmpty() || !storageService.isEnabled()) {
            return new MineruImageProcessingResult(parsePackage.markdown(), List.of());
        }
        imageAssetMapper.deleteByDocVersion(document.getTenantId(), document.getDocumentId(), document.getDocVersion());
        String username = resolveUsername(document);
        Map<String, MineruImageEntry> imagesByPath = parsePackage.imagesByNormalizedPath();
        Matcher matcher = MARKDOWN_IMAGE_PATTERN.matcher(parsePackage.markdown());
        StringBuffer rewritten = new StringBuffer();
        List<KbDocumentImageAssetEntity> assets = new ArrayList<>();
        int captionCount = 0;
        while (matcher.find()) {
            String originalAlt = matcher.group(1);
            String markdownPath = matcher.group(2).trim();
            MineruImageEntry image = parsePackage.findImage(markdownPath);
            if (image == null) {
                matcher.appendReplacement(rewritten, Matcher.quoteReplacement(matcher.group(0)));
                continue;
            }
            KbDocumentImageAssetEntity asset = handleImage(document, username, markdownPath, image, captionCount);
            if (asset.getCaptionStatus() == ImageCaptionStatus.SUCCESS) {
                captionCount++;
            }
            assets.add(asset);
            String alt = StringUtils.hasText(asset.getDescription()) ? asset.getDescription() : originalAlt;
            String url = StringUtils.hasText(asset.getPublicUrl()) ? asset.getPublicUrl() : markdownPath;
            matcher.appendReplacement(rewritten, Matcher.quoteReplacement("![" + alt + "](" + url + ")"));
            imagesByPath.remove(image.normalizedPath());
        }
        matcher.appendTail(rewritten);
        return new MineruImageProcessingResult(rewritten.toString(), assets);
    }

    private KbDocumentImageAssetEntity handleImage(KbDocumentEntity document,
                                                   String username,
                                                   String markdownPath,
                                                   MineruImageEntry image,
                                                   int captionCount) {
        KbDocumentImageAssetEntity asset = baseAsset(document, username, markdownPath, image);
        try {
            OssObjectReference reference = storageService.upload(
                    document.getBizType(),
                    username,
                    document.getOwnerUserId(),
                    document.getTenantId(),
                    document.getDocumentId(),
                    document.getDocVersion(),
                    image
            );
            asset.setOssBucket(reference.bucket());
            asset.setOssObjectKey(reference.objectKey());
            asset.setPublicUrl(reference.publicUrl());
            if (captionCount < properties.getMineru().getImageCaption().getMaxImagesPerDocument() && !captionService.shouldSkipCaption(image)) {
                String description = captionService.describe(image);
                asset.setDescription(description);
                asset.setCaptionStatus(captionService.isDecorativeDescription(description) ? ImageCaptionStatus.SKIPPED : ImageCaptionStatus.SUCCESS);
                asset.setCaptionError(asset.getCaptionStatus() == ImageCaptionStatus.SKIPPED ? "装饰性图片" : null);
            } else {
                asset.setDescription("图片");
                asset.setCaptionStatus(ImageCaptionStatus.SKIPPED);
                asset.setCaptionError("图片过小或超过图生文处理数量限制");
            }
        } catch (RuntimeException ex) {
            if (!properties.getMineru().getImageAsset().isContinueOnFailure()) {
                throw ex;
            }
            log.warn("MinerU 图片资产处理失败，已降级保留文档解析。documentId={}, path={}, error={}", document.getDocumentId(), image.originalPath(), ex.getMessage());
            asset.setDescription("图片");
            asset.setCaptionStatus(ImageCaptionStatus.FAILED);
            asset.setCaptionError(safeError(ex.getMessage()));
        }
        imageAssetMapper.insert(asset);
        return asset;
    }

    private KbDocumentImageAssetEntity baseAsset(KbDocumentEntity document, String username, String markdownPath, MineruImageEntry image) {
        KbDocumentImageAssetEntity asset = new KbDocumentImageAssetEntity();
        asset.setTenantId(document.getTenantId());
        asset.setDocumentId(document.getDocumentId());
        asset.setDocVersion(document.getDocVersion());
        asset.setBizType(document.getBizType());
        asset.setOwnerUserId(document.getOwnerUserId());
        asset.setUsername(username);
        asset.setOriginalPath(markdownPath);
        asset.setMimeType(image.mimeType());
        asset.setFileExt(image.fileExt());
        asset.setSizeBytes((long) image.bytes().length);
        asset.setContentHash(storageService.sha256Hex(image.bytes()));
        asset.setCaptionStatus(ImageCaptionStatus.PENDING);
        asset.setCreatedAt(LocalDateTime.now());
        asset.setUpdatedAt(LocalDateTime.now());
        return asset;
    }

    private String resolveUsername(KbDocumentEntity document) {
        LegalUserEntity user = legalUserMapper.selectOne(new LambdaQueryWrapper<LegalUserEntity>()
                .eq(LegalUserEntity::getTenantId, document.getTenantId())
                .eq(LegalUserEntity::getUserId, document.getOwnerUserId())
                .last("limit 1"));
        return user == null || !StringUtils.hasText(user.getUsername()) ? "user" : user.getUsername();
    }

    private String safeError(String message) {
        String value = StringUtils.hasText(message) ? message.trim() : "图片处理失败";
        return value.length() > 1000 ? value.substring(0, 1000) : value;
    }
}
