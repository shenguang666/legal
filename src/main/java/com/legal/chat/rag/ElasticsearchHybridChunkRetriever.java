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

        // 基于 RRF 的混合检索：向量(kNN) + BM25。
        // 不再强制返回 topK：由 topK 控制“最多返回多少条”，不足不补齐。
        int fetchK = Math.max(1, Math.max(topK, 20));
        double minSimilarity = ragProperties.getMinVectorSimilarity();
        // 注意：hybridSearchRrf 内部会做向量阈值过滤，并用 rrfMerge 合并后再截断。
        List<ChunkSearchHit> filtered = elasticsearchChunkStore.hybridSearchRrf(tenantId, question, queryVector, minSimilarity, fetchK)
                .stream()
                .limit(topK)
                .toList();

        // 2B 指标（无标签口径）：
        // precision = 过滤后条数 / 实际返回条数（这里相等，通常为 1；保留打印用于解释“有效命中率”）
        // recall = 是否至少返回 1 条（0/1）
        int retrievedCount = fetchK;
        int returnedCount = filtered.size();
        //TODO 这里的召回率和精准率写的不对，后期要改
        double precision = returnedCount == 0 ? 0D : 1D;
        double recall = returnedCount > 0 ? 1D : 0D;
        int latencyMs = (int) (System.currentTimeMillis() - start);

        String traceId = MDC.get("traceId");
        log.info("RAG retrieve(rrf) traceId={} tenantId={} topK={} fetchK={} threshold={} retrieved={} returned={} recall={} precision={} latencyMs={} question={}",
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
