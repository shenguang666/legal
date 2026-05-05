package com.legal.chat.rag;

import com.legal.chat.dto.CitationDto;

import java.util.List;

public class RagAnswer {

    private final String answer;
    private final String modelName;
    private final int tokenUsage;
    private final List<RetrievedChunk> retrievedChunks;
    private final List<RetrievedChunk> rawRetrievedChunks;
    private final List<CitationDto> citations;
    private final boolean knowledgeHit;

    public RagAnswer(String answer,
                     String modelName,
                     int tokenUsage,
                     List<RetrievedChunk> retrievedChunks,
                     List<RetrievedChunk> rawRetrievedChunks,
                     List<CitationDto> citations,
                     boolean knowledgeHit) {
        this.answer = answer;
        this.modelName = modelName;
        this.tokenUsage = tokenUsage;
        this.retrievedChunks = retrievedChunks;
        this.rawRetrievedChunks = rawRetrievedChunks;
        this.citations = citations;
        this.knowledgeHit = knowledgeHit;
    }

    public String getAnswer() {
        return answer;
    }

    public String getModelName() {
        return modelName;
    }

    public int getTokenUsage() {
        return tokenUsage;
    }

    public List<RetrievedChunk> getRetrievedChunks() {
        return retrievedChunks;
    }

    public List<RetrievedChunk> getRawRetrievedChunks() {
        return rawRetrievedChunks;
    }

    public List<CitationDto> getCitations() {
        return citations;
    }

    public boolean isKnowledgeHit() {
        return knowledgeHit;
    }
}
