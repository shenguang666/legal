package com.legal.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.legal.common.AppException;
import com.legal.config.ElasticsearchProperties;
import com.legal.knowledge.entity.KbChunkEntity;
import com.legal.knowledge.entity.KbDocumentEntity;
import com.legal.knowledge.entity.KbIndexOutboxEntity;
import com.legal.knowledge.mapper.KbChunkMapper;
import com.legal.knowledge.mapper.KbDocumentMapper;
import com.legal.retrieval.service.ChunkIndexPayload;
import com.legal.retrieval.service.ElasticsearchChunkStore;
import com.legal.retrieval.service.OpenAiEmbeddingClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class KnowledgeIndexService {

    private final KbDocumentMapper kbDocumentMapper;
    private final KbChunkMapper kbChunkMapper;
    private final ElasticsearchChunkStore elasticsearchChunkStore;
    private final OpenAiEmbeddingClient embeddingClient;
    private final ElasticsearchProperties elasticsearchProperties;

    public KnowledgeIndexService(KbDocumentMapper kbDocumentMapper,
                                 KbChunkMapper kbChunkMapper,
                                 ElasticsearchChunkStore elasticsearchChunkStore,
                                 OpenAiEmbeddingClient embeddingClient,
                                 ElasticsearchProperties elasticsearchProperties) {
        this.kbDocumentMapper = kbDocumentMapper;
        this.kbChunkMapper = kbChunkMapper;
        this.elasticsearchChunkStore = elasticsearchChunkStore;
        this.embeddingClient = embeddingClient;
        this.elasticsearchProperties = elasticsearchProperties;
    }

    public boolean processTask(KbIndexOutboxEntity task) {
        if ("UPSERT".equalsIgnoreCase(task.getOp())) {
            return processUpsert(task);
        }
        if ("DELETE".equalsIgnoreCase(task.getOp())) {
            processDelete(task);
            return true;
        }
        throw AppException.badRequest("不支持的索引任务类型: " + task.getOp());
    }

    public void markDocumentFailed(KbIndexOutboxEntity task) {
        KbDocumentEntity document = findDocument(task.getTenantId(), task.getDocumentId());
        if (document == null) {
            return;
        }
        document.setIndexStatus("FAILED");
        document.setUpdatedAt(LocalDateTime.now());
        kbDocumentMapper.updateById(document);
    }

    private boolean processUpsert(KbIndexOutboxEntity task) {
        KbDocumentEntity document = findDocument(task.getTenantId(), task.getDocumentId());
        if (document == null) {
            throw AppException.notFound("文档不存在，无法建立索引");
        }
        if (document.getDocVersion() > task.getDocVersion()) {
            return false;
        }

        List<KbChunkEntity> chunks = kbChunkMapper.selectByDocVersion(task.getTenantId(), task.getDocumentId(), task.getDocVersion());
        if (chunks.isEmpty()) {
            throw AppException.badRequest("文档无切片内容，无法建立索引");
        }

        List<ChunkIndexPayload> payloads = new ArrayList<>(chunks.size());
        String source = document.getTitle() + "（" + document.getSource() + "）";
        for (KbChunkEntity chunk : chunks) {
            List<Float> vector = embeddingClient.embed(chunk.getContent());
            int expectedDims = elasticsearchProperties.getIndex().getVectorDims();
            if (vector.size() != expectedDims) {
                throw new AppException(50021, 500,
                        "Embedding 向量维度不匹配，期望 " + expectedDims + "，实际 " + vector.size());
            }
            payloads.add(new ChunkIndexPayload(
                    chunk.getChunkId(),
                    chunk.getTenantId(),
                    chunk.getDocumentId(),
                    chunk.getDocVersion(),
                    chunk.getChunkOrder(),
                    source,
                    chunk.getContent(),
                    vector,
                    LocalDateTime.now()
            ));
        }

        elasticsearchChunkStore.deleteByDocument(task.getTenantId(), task.getDocumentId());
        elasticsearchChunkStore.upsertChunks(payloads);
        document.setStatus("ACTIVE");
        document.setIndexStatus("COMPLETED");
        document.setUpdatedAt(LocalDateTime.now());
        kbDocumentMapper.updateById(document);
        return true;
    }

    private void processDelete(KbIndexOutboxEntity task) {
        elasticsearchChunkStore.deleteByDocument(task.getTenantId(), task.getDocumentId());
        KbDocumentEntity document = findDocument(task.getTenantId(), task.getDocumentId());
        if (document == null) {
            return;
        }
        if (document.getDocVersion() <= task.getDocVersion()) {
            document.setIndexStatus("COMPLETED");
            document.setUpdatedAt(LocalDateTime.now());
            kbDocumentMapper.updateById(document);
        }
    }

    private KbDocumentEntity findDocument(Long tenantId, Long documentId) {
        return kbDocumentMapper.selectOne(
                new LambdaQueryWrapper<KbDocumentEntity>()
                        .eq(KbDocumentEntity::getTenantId, tenantId)
                        .eq(KbDocumentEntity::getDocumentId, documentId)
                        .last("limit 1")
        );
    }
}
