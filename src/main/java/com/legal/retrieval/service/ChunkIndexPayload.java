package com.legal.retrieval.service;

import com.legal.enums.KbChunkType;

import java.time.LocalDateTime;
import java.util.List;

public class ChunkIndexPayload {

    private final Long chunkId;
    private final Long tenantId;
    private final Long documentId;
    private final Integer docVersion;
    private final Integer chunkOrder;
    private final String source;
    private final String content;
    private final KbChunkType chunkType;
    private final Long parentChunkId;
    private final List<Float> contentVector;
    private final LocalDateTime updatedAt;

    public ChunkIndexPayload(Long chunkId,
                             Long tenantId,
                             Long documentId,
                             Integer docVersion,
                             Integer chunkOrder,
                             String source,
                             String content,
                             KbChunkType chunkType,
                             Long parentChunkId,
                             List<Float> contentVector,
                             LocalDateTime updatedAt) {
        this.chunkId = chunkId;
        this.tenantId = tenantId;
        this.documentId = documentId;
        this.docVersion = docVersion;
        this.chunkOrder = chunkOrder;
        this.source = source;
        this.content = content;
        this.chunkType = chunkType;
        this.parentChunkId = parentChunkId;
        this.contentVector = contentVector;
        this.updatedAt = updatedAt;
    }

    public Long getChunkId() {
        return chunkId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public Integer getDocVersion() {
        return docVersion;
    }

    public Integer getChunkOrder() {
        return chunkOrder;
    }

    public String getSource() {
        return source;
    }

    public String getContent() {
        return content;
    }

    public KbChunkType getChunkType() {
        return chunkType;
    }

    public Long getParentChunkId() {
        return parentChunkId;
    }

    public List<Float> getContentVector() {
        return contentVector;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
