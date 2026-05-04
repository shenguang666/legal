package com.legal.knowledge.service;

import com.legal.knowledge.entity.KbDocumentImageAssetEntity;

import java.util.List;

/**
 * MinerU 图片资产处理结果。
 */
public record MineruImageProcessingResult(
        String markdown,
        List<KbDocumentImageAssetEntity> assets
) {

    public MineruImageProcessingResult {
        assets = assets == null ? List.of() : List.copyOf(assets);
    }
}
