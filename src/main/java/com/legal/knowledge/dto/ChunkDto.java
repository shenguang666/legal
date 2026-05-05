package com.legal.knowledge.dto;

import com.legal.enums.KbChunkType;

public class ChunkDto {

    private Long chunkId;
    private Integer chunkOrder;
    private KbChunkType chunkType;
    private Long parentChunkId;
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

    public KbChunkType getChunkType() {
        return chunkType;
    }

    public void setChunkType(KbChunkType chunkType) {
        this.chunkType = chunkType;
    }

    public Long getParentChunkId() {
        return parentChunkId;
    }

    public void setParentChunkId(Long parentChunkId) {
        this.parentChunkId = parentChunkId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
