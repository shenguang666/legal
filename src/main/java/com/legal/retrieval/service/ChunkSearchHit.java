package com.legal.retrieval.service;

import com.legal.enums.KbChunkType;

public class ChunkSearchHit {

    private final Long chunkId;
    private final Long documentId;
    private final Integer chunkOrder;
    private final String source;
    private final String content;
    private final KbChunkType chunkType;
    private final Long parentChunkId;
    private final double score;

    public ChunkSearchHit(Long chunkId,
                          Long documentId,
                          Integer chunkOrder,
                          String source,
                          String content,
                          KbChunkType chunkType,
                          Long parentChunkId,
                          double score) {
        this.chunkId = chunkId;
        this.documentId = documentId;
        this.chunkOrder = chunkOrder;
        this.source = source;
        this.content = content;
        this.chunkType = chunkType;
        this.parentChunkId = parentChunkId;
        this.score = score;
    }

    public Long getChunkId() {
        return chunkId;
    }

    public Long getDocumentId() {
        return documentId;
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

    public double getScore() {
        return score;
    }
}
