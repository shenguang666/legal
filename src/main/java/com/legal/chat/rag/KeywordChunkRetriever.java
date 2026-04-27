package com.legal.chat.rag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.legal.knowledge.entity.KbDocumentEntity;
import com.legal.knowledge.entity.KbChunkEntity;
import com.legal.knowledge.mapper.KbChunkMapper;
import com.legal.knowledge.mapper.KbDocumentMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class KeywordChunkRetriever implements ChunkRetriever {

    private final KbChunkMapper kbChunkMapper;
    private final KbDocumentMapper kbDocumentMapper;

    public KeywordChunkRetriever(KbChunkMapper kbChunkMapper, KbDocumentMapper kbDocumentMapper) {
        this.kbChunkMapper = kbChunkMapper;
        this.kbDocumentMapper = kbDocumentMapper;
    }

    @Override
    public List<RetrievedChunk> retrieve(Long tenantId, String question, int topK) {
        List<String> keywords = extractKeywords(question);
        List<KbChunkEntity> chunks = kbChunkMapper.searchByKeywords(tenantId, keywords, topK);
        if (chunks.isEmpty()) {
            return List.of();
        }

        Map<Long, KbDocumentEntity> documents = kbDocumentMapper.selectList(
                        new LambdaQueryWrapper<KbDocumentEntity>()
                                .eq(KbDocumentEntity::getTenantId, tenantId)
                                .in(KbDocumentEntity::getDocumentId,
                                        chunks.stream().map(KbChunkEntity::getDocumentId).distinct().toList())
                ).stream()
                .collect(Collectors.toMap(KbDocumentEntity::getDocumentId, document -> document, (left, right) -> left, LinkedHashMap::new));

        List<RetrievedChunk> results = new ArrayList<>();
        for (KbChunkEntity chunk : chunks) {
            KbDocumentEntity document = documents.get(chunk.getDocumentId());
            String source = document == null ? "知识库文档" : document.getTitle() + "（" + document.getSource() + "）";
            results.add(new RetrievedChunk(
                    chunk.getChunkId(),
                    chunk.getDocumentId(),
                    chunk.getChunkOrder(),
                    source,
                    chunk.getContent()
            ));
        }
        return results;
    }

    private List<String> extractKeywords(String question) {
        if (!StringUtils.hasText(question)) {
            return List.of();
        }
        return Arrays.stream(question.trim().split("\\s+|，|。|；|、|,|\\?|？"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .filter(word -> word.length() >= 2)
                .distinct()
                .limit(8)
                .toList();
    }
}
