package com.legal.chat.rag;

import com.legal.enums.KbChunkType;

public class RetrievedChunk {

    private final Long chunkId;
    private final Long documentId;
    private final Integer chunkOrder;
    private final String source;
    private final String content;
    private final KbChunkType chunkType;
    private final Long parentChunkId;

    public RetrievedChunk(Long chunkId, Long documentId, Integer chunkOrder, String source, String content) {
        this(chunkId, documentId, chunkOrder, source, content, KbChunkType.NORMAL, null);
    }

    public RetrievedChunk(Long chunkId,
                          Long documentId,
                          Integer chunkOrder,
                          String source,
                          String content,
                          KbChunkType chunkType,
                          Long parentChunkId) {
        this.chunkId = chunkId;
        this.documentId = documentId;
        this.chunkOrder = chunkOrder;
        this.source = source;
        this.content = content;
        this.chunkType = chunkType == null ? KbChunkType.NORMAL : chunkType;
        this.parentChunkId = parentChunkId;
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
}
