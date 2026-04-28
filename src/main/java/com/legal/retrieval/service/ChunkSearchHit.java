package com.legal.retrieval.service;

public class ChunkSearchHit {

    private final Long chunkId;
    private final Long documentId;
    private final Integer chunkOrder;
    private final String source;
    private final String content;
    private final double score;

    public ChunkSearchHit(Long chunkId,
                          Long documentId,
                          Integer chunkOrder,
                          String source,
                          String content,
                          double score) {
        this.chunkId = chunkId;
        this.documentId = documentId;
        this.chunkOrder = chunkOrder;
        this.source = source;
        this.content = content;
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

    public double getScore() {
        return score;
    }
}
