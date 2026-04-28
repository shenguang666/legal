package com.legal.retrieval.dto;

public class RetrievedChunkDto {

    private Long chunkId;
    private Long documentId;
    private Integer chunkOrder;
    private String source;
    private String content;

    public Long getChunkId() {
        return chunkId;
    }

    public void setChunkId(Long chunkId) {
        this.chunkId = chunkId;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public Integer getChunkOrder() {
        return chunkOrder;
    }

    public void setChunkOrder(Integer chunkOrder) {
        this.chunkOrder = chunkOrder;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
