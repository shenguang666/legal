package com.legal.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.legal.common.AppException;
import com.legal.config.ElasticsearchProperties;
import com.legal.enums.DocumentParseMethod;
import com.legal.enums.KbDocumentBizType;
import com.legal.enums.KbDocumentStatus;
import com.legal.enums.KbIndexStatus;
import com.legal.enums.KbChunkType;
import com.legal.enums.KbOutboxOp;
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
        if (task.getOp() == KbOutboxOp.UPSERT) {
            return processUpsert(task);
        }
        if (task.getOp() == KbOutboxOp.DELETE) {
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
        document.setIndexStatus(KbIndexStatus.FAILED);
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
        String targetIndex = resolveIndexName(document);
        for (KbChunkEntity chunk : chunks) {
            KbChunkType chunkType = chunk.getChunkType() == null ? KbChunkType.NORMAL : chunk.getChunkType();
            if (chunkType == KbChunkType.PARENT) {
                continue;
            }
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
                    chunkType,
                    chunk.getParentChunkId(),
                    vector,
                    LocalDateTime.now()
            ));
        }
        if (payloads.isEmpty()) {
            throw AppException.badRequest("文档无可索引切片内容，无法建立索引");
        }

        deleteKnowledgeDocumentFromAllIndexes(task.getTenantId(), task.getDocumentId(), document.getBizType());
        elasticsearchChunkStore.upsertChunks(payloads, targetIndex);
        document.setStatus(KbDocumentStatus.ACTIVE);
        document.setIndexStatus(KbIndexStatus.COMPLETED);
        document.setUpdatedAt(LocalDateTime.now());
        kbDocumentMapper.updateById(document);
        return true;
    }

    private void processDelete(KbIndexOutboxEntity task) {
        KbDocumentEntity document = findDocument(task.getTenantId(), task.getDocumentId());
        if (document == null) {
            return;
        }
        deleteKnowledgeDocumentFromAllIndexes(task.getTenantId(), task.getDocumentId(), document.getBizType());
        if (document.getDocVersion() <= task.getDocVersion()) {
            document.setIndexStatus(KbIndexStatus.COMPLETED);
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

    private String resolveIndexName(KbDocumentEntity document) {
        KbDocumentBizType bizType = document.getBizType();
        if (bizType == KbDocumentBizType.RISK_RULE) {
            return elasticsearchProperties.getIndex().getRiskRule();
        }
        if (bizType == KbDocumentBizType.KNOWLEDGE || bizType == null) {
            if (document.getParseMethod() == DocumentParseMethod.MINERU_PRECISE) {
                return elasticsearchProperties.getIndex().getKbChunksMineru();
            }
            return elasticsearchProperties.getIndex().getKbChunks();
        }
        throw AppException.badRequest("当前文档类型不支持索引: " + bizType.getCode());
    }

    private void deleteKnowledgeDocumentFromAllIndexes(Long tenantId, Long documentId, KbDocumentBizType bizType) {
        if (bizType == KbDocumentBizType.KNOWLEDGE || bizType == null) {
            elasticsearchChunkStore.deleteByDocument(tenantId, documentId, elasticsearchProperties.getIndex().getKbChunks());
            elasticsearchChunkStore.deleteByDocument(tenantId, documentId, elasticsearchProperties.getIndex().getKbChunksMineru());
            return;
        }
        elasticsearchChunkStore.deleteByDocument(tenantId, documentId, elasticsearchProperties.getIndex().getRiskRule());
    }
}
