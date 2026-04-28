package com.legal.chat.rag;

import com.legal.retrieval.service.ChunkSearchHit;
import com.legal.retrieval.service.ElasticsearchChunkStore;
import com.legal.retrieval.service.OpenAiEmbeddingClient;
import com.legal.config.RagProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

@Primary
@Component
public class ElasticsearchHybridChunkRetriever implements ChunkRetriever {

    private final ElasticsearchChunkStore elasticsearchChunkStore;
    private final OpenAiEmbeddingClient embeddingClient;
    private final KeywordChunkRetriever keywordChunkRetriever;
    private final RagProperties ragProperties;

    public ElasticsearchHybridChunkRetriever(ElasticsearchChunkStore elasticsearchChunkStore,
                                             OpenAiEmbeddingClient embeddingClient,
                                             KeywordChunkRetriever keywordChunkRetriever,
                                             RagProperties ragProperties) {
        this.elasticsearchChunkStore = elasticsearchChunkStore;
        this.embeddingClient = embeddingClient;
        this.keywordChunkRetriever = keywordChunkRetriever;
        this.ragProperties = ragProperties;
    }

    @Override
    public List<RetrievedChunk> retrieve(Long tenantId, String question, int topK) {
        if (!elasticsearchChunkStore.isEnabled()) {
            return keywordChunkRetriever.retrieve(tenantId, question, topK);
        }
        List<Float> queryVector = embeddingClient.embed(question);
        // 不再强制返回 topK：先尽量多召回，再按最小相似度阈值过滤后截断。
        int fetchK = Math.max(1, Math.max(topK, 20));
        List<ChunkSearchHit> hits = elasticsearchChunkStore.hybridSearch(tenantId, question, queryVector, fetchK);
        double minSimilarity = ragProperties.getMinVectorSimilarity();
        hits = hits.stream()
                .filter(hit -> hit.getScore() >= minSimilarity)
                .limit(topK)
                .toList();
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
