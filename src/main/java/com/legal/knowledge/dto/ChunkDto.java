package com.legal.knowledge.dto;

public class ChunkDto {

    private Long chunkId;
    private Integer chunkOrder;
    private String content;

    public Long getChunkId() {
        return chunkId;
    }

    public void setChunkId(Long chunkId) {
        this.chunkId = chunkId;
    }

    public Integer getChunkOrder() {
        return chunkOrder;
    }

    public void setChunkOrder(Integer chunkOrder) {
        this.chunkOrder = chunkOrder;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
