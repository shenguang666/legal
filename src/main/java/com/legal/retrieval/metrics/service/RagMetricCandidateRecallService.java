package com.legal.retrieval.metrics.service;

import com.legal.chat.rag.ChunkRetriever;
import com.legal.chat.rag.RetrievedChunk;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RagMetricCandidateRecallService {

    private final ChunkRetriever chunkRetriever;

    public RagMetricCandidateRecallService(ChunkRetriever chunkRetriever) {
        this.chunkRetriever = chunkRetriever;
    }

    public List<RetrievedChunk> recall(Long tenantId, String queryText, int topN) {
        int safeTopN = Math.max(1, topN);
        return chunkRetriever.retrieve(tenantId, queryText, safeTopN);
    }
}
