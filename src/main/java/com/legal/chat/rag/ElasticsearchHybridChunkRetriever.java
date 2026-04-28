package com.legal.chat.rag;

import com.legal.retrieval.service.ChunkSearchHit;
import com.legal.retrieval.service.ElasticsearchChunkStore;
import com.legal.retrieval.service.OpenAiEmbeddingClient;
import com.legal.config.RagProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

@Primary
@Component
public class ElasticsearchHybridChunkRetriever implements ChunkRetriever {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchHybridChunkRetriever.class);

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
        long start = System.currentTimeMillis();
        List<Float> queryVector = embeddingClient.embed(question);

        // 2B 口径：不再强制返回 topK。
        // 先做向量粗召回（fetchK），再按相似度阈值过滤并截断到 topK。
        int fetchK = Math.max(1, Math.max(topK, 20));
        List<ChunkSearchHit> raw = elasticsearchChunkStore.vectorSearch(tenantId, queryVector, fetchK);
        double minSimilarity = ragProperties.getMinVectorSimilarity();
        List<ChunkSearchHit> filtered = raw.stream()
                .filter(hit -> hit.getScore() >= minSimilarity)
                .limit(topK)
                .toList();

        // 2B 指标（无标签口径）：
        // precision = 过滤后条数 / 实际返回条数（这里相等，通常为 1；保留打印用于解释“有效命中率”）
        // recall = 是否至少返回 1 条（0/1）
        int retrievedCount = raw.size();
        int returnedCount = filtered.size();
        double precision = returnedCount == 0 ? 0D : 1D;
        double recall = returnedCount > 0 ? 1D : 0D;
        int latencyMs = (int) (System.currentTimeMillis() - start);

        String traceId = MDC.get("traceId");
        log.info("RAG retrieve(vector) traceId={} tenantId={} topK={} fetchK={} threshold={} retrieved={} returned={} recall={} precision={} latencyMs={} question={}",
                traceId,
                tenantId,
                topK,
                fetchK,
                String.format("%.2f", minSimilarity),
                retrievedCount,
                returnedCount,
                String.format("%.2f", recall),
                String.format("%.2f", precision),
                latencyMs,
                shorten(question, 120));

        List<ChunkSearchHit> hits = filtered;
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

    private String shorten(String text, int maxLen) {
        if (text == null) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLen) {
            return normalized;
        }
        return normalized.substring(0, maxLen) + "...";
    }
}
