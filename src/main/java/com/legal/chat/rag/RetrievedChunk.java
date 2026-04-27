package com.legal.chat.rag;

public class RetrievedChunk {

    private final Long chunkId;
    private final Long documentId;
    private final Integer chunkOrder;
    private final String source;
    private final String content;

    public RetrievedChunk(Long chunkId, Long documentId, Integer chunkOrder, String source, String content) {
        this.chunkId = chunkId;
        this.documentId = documentId;
        this.chunkOrder = chunkOrder;
        this.source = source;
        this.content = content;
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
}
