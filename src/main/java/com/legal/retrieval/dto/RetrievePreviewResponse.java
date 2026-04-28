package com.legal.retrieval.dto;

import java.util.List;

public class RetrievePreviewResponse {

    private List<RetrievedChunkDto> chunks;

    public RetrievePreviewResponse() {
    }

    public RetrievePreviewResponse(List<RetrievedChunkDto> chunks) {
        this.chunks = chunks;
    }

    public List<RetrievedChunkDto> getChunks() {
        return chunks;
    }

    public void setChunks(List<RetrievedChunkDto> chunks) {
        this.chunks = chunks;
    }
}
