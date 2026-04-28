package com.legal.chat.rag;

import com.legal.retrieval.service.ChunkSearchHit;
import com.legal.retrieval.service.ElasticsearchChunkStore;
import com.legal.retrieval.service.OpenAiEmbeddingClient;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

@Primary
@Component
public class ElasticsearchHybridChunkRetriever implements ChunkRetriever {

    private final ElasticsearchChunkStore elasticsearchChunkStore;
    private final OpenAiEmbeddingClient embeddingClient;
    private final KeywordChunkRetriever keywordChunkRetriever;

    public ElasticsearchHybridChunkRetriever(ElasticsearchChunkStore elasticsearchChunkStore,
                                             OpenAiEmbeddingClient embeddingClient,
                                             KeywordChunkRetriever keywordChunkRetriever) {
        this.elasticsearchChunkStore = elasticsearchChunkStore;
        this.embeddingClient = embeddingClient;
        this.keywordChunkRetriever = keywordChunkRetriever;
    }

    @Override
    public List<RetrievedChunk> retrieve(Long tenantId, String question, int topK) {
        if (!elasticsearchChunkStore.isEnabled()) {
            return keywordChunkRetriever.retrieve(tenantId, question, topK);
        }
        List<Float> queryVector = embeddingClient.embed(question);
        List<ChunkSearchHit> hits = elasticsearchChunkStore.hybridSearch(tenantId, question, queryVector, topK);
        return hits.stream()
                .map(hit -> new RetrievedChunk(
                        hit.getChunkId(),
                        hit.getDocumentId(),
                        hit.getChunkOrder(),
                        hit.getSource(),
                        hit.getContent()
                ))
                .toList();
    }
}
