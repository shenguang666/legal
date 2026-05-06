package com.legal.court.service;

import com.legal.chat.rag.ChunkRetriever;
import com.legal.chat.rag.RetrievedChunk;
import com.legal.court.dto.CourtEvidenceContext;
import com.legal.enums.KbChunkType;
import com.legal.knowledge.entity.KbChunkEntity;
import com.legal.knowledge.mapper.KbChunkMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 智能小法庭庭审证据上下文服务。
 */
@Slf4j
@Service
public class CourtEvidenceContextService {

    private final ChunkRetriever chunkRetriever;
    private final KbChunkMapper kbChunkMapper;

    public CourtEvidenceContextService(ChunkRetriever chunkRetriever, KbChunkMapper kbChunkMapper) {
        this.chunkRetriever = chunkRetriever;
        this.kbChunkMapper = kbChunkMapper;
    }

    /**
     * 按智能小法庭规则检索并强制父级上下文扩展，父分块缺失的命中会被跳过。
     */
    public CourtEvidenceContext retrieveForCourt(Long tenantId, String question, int topK) {
        CourtEvidenceContext context = new CourtEvidenceContext();
        List<RetrievedChunk> rawChunks = chunkRetriever.retrieve(tenantId, question, topK);
        if (rawChunks == null || rawChunks.isEmpty()) {
            return context;
        }
        return buildContext(tenantId, rawChunks);
    }

    /**
     * 对已有召回结果应用智能小法庭父级扩展规则，便于复用上层检索结果。
     */
    public CourtEvidenceContext expandForCourt(Long tenantId, List<RetrievedChunk> rawChunks) {
        CourtEvidenceContext context = new CourtEvidenceContext();
        if (rawChunks == null || rawChunks.isEmpty()) {
            return context;
        }
        return buildContext(tenantId, rawChunks);
    }

    private CourtEvidenceContext buildContext(Long tenantId, List<RetrievedChunk> rawChunks) {
        CourtEvidenceContext context = new CourtEvidenceContext();
        Set<Long> parentIds = rawChunks.stream()
                .filter(chunk -> chunk.getChunkType() == KbChunkType.CHILD)
                .map(RetrievedChunk::getParentChunkId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, KbChunkEntity> parentsById = loadParentChunks(tenantId, parentIds);
        Set<Long> emittedParentIds = new LinkedHashSet<>();
        for (RetrievedChunk chunk : rawChunks) {
            if (chunk.getChunkType() == KbChunkType.CHILD) {
                Long parentChunkId = chunk.getParentChunkId();
                KbChunkEntity parent = parentChunkId == null ? null : parentsById.get(parentChunkId);
                if (parent == null) {
                    context.getSkippedChildChunkIds().add(chunk.getChunkId());
                    log.warn("智能小法庭父分块缺失，跳过子分块 tenantId={} childChunkId={} parentChunkId={}",
                            tenantId, chunk.getChunkId(), parentChunkId);
                    continue;
                }
                if (emittedParentIds.add(parent.getChunkId())) {
                    context.getItems().add(toItem(chunk, parent));
                }
            } else {
                if (emittedParentIds.add(chunk.getChunkId())) {
                    context.getItems().add(toItem(chunk, null));
                }
            }
        }
        return context;
    }

    private CourtEvidenceContext.Item toItem(RetrievedChunk hit, KbChunkEntity parent) {
        CourtEvidenceContext.Item item = new CourtEvidenceContext.Item();
        if (parent != null) {
            item.setParentChunkId(parent.getChunkId());
            item.setHitChildChunkId(hit.getChunkId());
            item.setDocumentId(parent.getDocumentId());
            item.setContent(parent.getContent());
        } else {
            item.setParentChunkId(hit.getChunkId());
            item.setHitChildChunkId(hit.getChunkId());
            item.setDocumentId(hit.getDocumentId());
            item.setContent(hit.getContent());
        }
        item.setSource(hit.getSource());
        return item;
    }

    private Map<Long, KbChunkEntity> loadParentChunks(Long tenantId, Set<Long> parentIds) {
        if (parentIds == null || parentIds.isEmpty()) {
            return Map.of();
        }
        List<KbChunkEntity> parents = kbChunkMapper.selectParentChunksByIds(tenantId, new ArrayList<>(parentIds));
        Map<Long, KbChunkEntity> result = new LinkedHashMap<>();
        for (KbChunkEntity parent : parents) {
            result.put(parent.getChunkId(), parent);
        }
        return result;
    }
}
