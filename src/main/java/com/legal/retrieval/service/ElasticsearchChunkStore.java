package com.legal.retrieval.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.legal.common.AppException;
import com.legal.config.ElasticsearchProperties;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ElasticsearchChunkStore {

    private static final MediaType NDJSON_MEDIA_TYPE = MediaType.parseMediaType("application/x-ndjson");

    private final ElasticsearchProperties properties;
    private final RestClient restClient;

    public ElasticsearchChunkStore(ElasticsearchProperties properties) {
        this.properties = properties;
        this.restClient = buildClient(properties);
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public void upsertChunks(List<ChunkIndexPayload> chunks) {
        if (!isEnabled()) {
            return;
        }
        if (chunks == null || chunks.isEmpty()) {
            return;
        }
        ensureIndex();

        StringBuilder ndjson = new StringBuilder(chunks.size() * 300);
        for (ChunkIndexPayload chunk : chunks) {
            ndjson.append("{\"index\":{\"_index\":\"")
                    .append(properties.getIndex().getKbChunks())
                    .append("\",\"_id\":\"")
                    .append(buildDocId(chunk.getTenantId(), chunk.getDocumentId(), chunk.getChunkId()))
                    .append("\"}}\n");
            ndjson.append(buildSourceDocument(chunk)).append('\n');
        }

        JsonNode response = restClient.post()
                .uri("/_bulk")
                .contentType(NDJSON_MEDIA_TYPE)
                .body(ndjson.toString().getBytes(StandardCharsets.UTF_8))
                .retrieve()
                .body(JsonNode.class);
        if (response != null && response.path("errors").asBoolean(false)) {
            throw new AppException(50011, 500, "Elasticsearch 批量写入失败");
        }
    }

    public void deleteByDocument(Long tenantId, Long documentId) {
        if (!isEnabled()) {
            return;
        }
        ensureIndex();
        Map<String, Object> body = Map.of(
                "query", Map.of(
                        "bool", Map.of(
                                "filter", List.of(
                                        Map.of("term", Map.of("tenant_id", tenantId)),
                                        Map.of("term", Map.of("document_id", documentId))
                                )
                        )
                )
        );
        restClient.post()
                .uri("/" + properties.getIndex().getKbChunks() + "/_delete_by_query")
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    public void deleteKbChunksIndex() {
        if (!isEnabled()) {
            return;
        }
        try {
            restClient.delete()
                    .uri("/" + properties.getIndex().getKbChunks())
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                return;
            }
            throw new AppException(50013, 500, "删除 Elasticsearch 索引失败: " + summarizeError(ex.getResponseBodyAsString()));
        }
    }

    public List<ChunkSearchHit> hybridSearch(Long tenantId,
                                             String question,
                                             List<Float> questionVector,
                                             int topK) {
        if (!isEnabled()) {
            return List.of();
        }
        ensureIndex();

        List<ChunkSearchHit> vectorHits = vectorSearch(tenantId, questionVector);
        List<ChunkSearchHit> bm25Hits = bm25Search(tenantId, question);
        return rrfMerge(vectorHits, bm25Hits, properties.getSearch().getRrfK(), topK);
    }

    /**
     * RRF 混合检索：向量(kNN) + 关键词(BM25)。
     *
     * @param minVectorSimilarity 向量召回最小相似度阈值（基于 ES _score），低于阈值的向量命中会被丢弃。
     * @param topK 返回最多 topK 条（不会强制补齐）。
     */
    public List<ChunkSearchHit> hybridSearchRrf(Long tenantId,
                                                String question,
                                                List<Float> questionVector,
                                                double minVectorSimilarity,
                                                int topK) {
        if (!isEnabled()) {
            return List.of();
        }
        ensureIndex();

        List<ChunkSearchHit> vectorHits = vectorSearch(tenantId, questionVector)
                .stream()
                .filter(hit -> hit.getScore() >= minVectorSimilarity)
                .toList();
        List<ChunkSearchHit> bm25Hits = bm25Search(tenantId, question);
        return rrfMerge(vectorHits, bm25Hits, properties.getSearch().getRrfK(), topK);
    }

    /**
     * 仅向量检索（kNN）。返回的 score 为 Elasticsearch _score（通常对应 cosine similarity）。
     */
    public List<ChunkSearchHit> vectorSearch(Long tenantId, List<Float> questionVector, int topK) {
        if (!isEnabled()) {
            return List.of();
        }
        ensureIndex();
        int safeTopK = Math.max(1, topK);
        return doVectorSearch(tenantId, questionVector, safeTopK);
    }

    private List<ChunkSearchHit> vectorSearch(Long tenantId, List<Float> questionVector) {
        int vectorTopK = Math.max(1, properties.getSearch().getVectorTopK());
        return doVectorSearch(tenantId, questionVector, vectorTopK);
    }

    private List<ChunkSearchHit> doVectorSearch(Long tenantId, List<Float> questionVector, int vectorTopK) {
        int numCandidates = Math.max(vectorTopK * 2, 100);
        Map<String, Object> body = Map.of(
                "size", vectorTopK,
                "knn", Map.of(
                        "field", "content_vector",
                        "query_vector", questionVector,
                        "k", vectorTopK,
                        "num_candidates", numCandidates,
                        "filter", Map.of("term", Map.of("tenant_id", tenantId))
                ),
                "_source", List.of("chunk_id", "document_id", "chunk_order", "source", "content")
        );
        JsonNode response = restClient.post()
                .uri("/" + properties.getIndex().getKbChunks() + "/_search")
                .body(body)
                .retrieve()
                .body(JsonNode.class);
        return parseHits(response);
    }

    private List<ChunkSearchHit> bm25Search(Long tenantId, String question) {
        int bm25TopK = Math.max(1, properties.getSearch().getBm25TopK());
        Map<String, Object> body = Map.of(
                "size", bm25TopK,
                "query", Map.of(
                        "bool", Map.of(
                                "filter", List.of(Map.of("term", Map.of("tenant_id", tenantId))),
                                "must", List.of(
                                        Map.of(
                                                "match",
                                                Map.of("content", Map.of("query", question))
                                        )
                                )
                        )
                ),
                "_source", List.of("chunk_id", "document_id", "chunk_order", "source", "content")
        );
        JsonNode response = restClient.post()
                .uri("/" + properties.getIndex().getKbChunks() + "/_search")
                .body(body)
                .retrieve()
                .body(JsonNode.class);
        return parseHits(response);
    }

    private List<ChunkSearchHit> parseHits(JsonNode response) {
        if (response == null) {
            return List.of();
        }
        JsonNode hits = response.path("hits").path("hits");
        if (!hits.isArray() || hits.isEmpty()) {
            return List.of();
        }
        List<ChunkSearchHit> result = new ArrayList<>(hits.size());
        for (JsonNode hitNode : hits) {
            JsonNode source = hitNode.path("_source");
            Long chunkId = asLong(source.path("chunk_id"));
            Long documentId = asLong(source.path("document_id"));
            Integer chunkOrder = asInt(source.path("chunk_order"));
            String sourceText = source.path("source").asText("知识库文档");
            String content = source.path("content").asText("");
            double score = hitNode.path("_score").asDouble(0D);
            if (chunkId == null || documentId == null) {
                continue;
            }
            result.add(new ChunkSearchHit(chunkId, documentId, chunkOrder, sourceText, content, score));
        }
        return result;
    }

    private List<ChunkSearchHit> rrfMerge(List<ChunkSearchHit> vectorHits,
                                          List<ChunkSearchHit> bm25Hits,
                                          int rrfK,
                                          int topK) {
        Map<Long, RrfHolder> merged = new LinkedHashMap<>();
        applyRrf(vectorHits, merged, rrfK);
        applyRrf(bm25Hits, merged, rrfK);
        return merged.values().stream()
                .sorted((left, right) -> Double.compare(right.score, left.score))
                .limit(topK)
                .map(holder -> new ChunkSearchHit(
                        holder.hit.getChunkId(),
                        holder.hit.getDocumentId(),
                        holder.hit.getChunkOrder(),
                        holder.hit.getSource(),
                        holder.hit.getContent(),
                        holder.score
                ))
                .collect(Collectors.toList());
    }

    private void applyRrf(List<ChunkSearchHit> hits, Map<Long, RrfHolder> merged, int rrfK) {
        int safeK = Math.max(1, rrfK);
        for (int i = 0; i < hits.size(); i++) {
            ChunkSearchHit hit = hits.get(i);
            double add = 1D / (safeK + i + 1D);
            merged.compute(hit.getChunkId(), (ignored, current) -> {
                if (current == null) {
                    return new RrfHolder(hit, add);
                }
                current.score += add;
                return current;
            });
        }
    }

    private void ensureIndex() {
        try {
            restClient.put()
                    .uri("/" + properties.getIndex().getKbChunks())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(buildIndexMapping())
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            String body = ex.getResponseBodyAsString();
            if (status == 400 && body != null && body.contains("resource_already_exists_exception")) {
                return;
            }
            throw new AppException(50012, 500, "初始化 Elasticsearch 索引失败: " + summarizeError(body));
        }
    }

    private String buildDocId(Long tenantId, Long documentId, Long chunkId) {
        return tenantId + ":" + documentId + ":" + chunkId;
    }

    private Object buildIndexMapping() {
        return Map.of(
                "mappings", Map.of(
                        "properties", Map.of(
                                "chunk_id", Map.of("type", "long"),
                                "tenant_id", Map.of("type", "long"),
                                "document_id", Map.of("type", "long"),
                                "doc_version", Map.of("type", "integer"),
                                "chunk_order", Map.of("type", "integer"),
                                "source", Map.of("type", "keyword"),
                                "content", Map.of("type", "text"),
                                "content_vector", Map.of(
                                        "type", "dense_vector",
                                        "dims", properties.getIndex().getVectorDims(),
                                        "index", true,
                                        "similarity", "cosine"
                                ),
                                "updated_at", Map.of("type", "date")
                        )
                )
        );
    }

    private String buildSourceDocument(ChunkIndexPayload chunk) {
        String vector = chunk.getContentVector().stream()
                .map(value -> String.format(Locale.ROOT, "%.8f", value))
                .collect(Collectors.joining(","));
        return "{\"chunk_id\":" + chunk.getChunkId()
                + ",\"tenant_id\":" + chunk.getTenantId()
                + ",\"document_id\":" + chunk.getDocumentId()
                + ",\"doc_version\":" + chunk.getDocVersion()
                + ",\"chunk_order\":" + chunk.getChunkOrder()
                + ",\"source\":\"" + escapeJson(chunk.getSource()) + "\""
                + ",\"content\":\"" + escapeJson(chunk.getContent()) + "\""
                + ",\"content_vector\":[" + vector + "]"
                + ",\"updated_at\":\"" + chunk.getUpdatedAt() + "\"}";
    }

    private RestClient buildClient(ElasticsearchProperties props) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Math.toIntExact(props.getConnectTimeout().toMillis()));
        requestFactory.setReadTimeout(Math.toIntExact(props.getSocketTimeout().toMillis()));

        return RestClient.builder()
                .baseUrl(props.getPrimaryUri())
                .requestFactory(requestFactory)
                .defaultHeaders(headers -> {
                    if (StringUtils.hasText(props.getUsername())) {
                        headers.setBasicAuth(props.getUsername(), props.getPassword() == null ? "" : props.getPassword());
                    }
                    headers.set("Content-Type", "application/json");
                })
                .build();
    }

    private static String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(text.length() + 16);
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            switch (ch) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        out.append(String.format(Locale.ROOT, "\\u%04x", (int) ch));
                    } else {
                        out.append(ch);
                    }
                }
            }
        }
        return out.toString();
    }

    private static Long asLong(JsonNode node) {
        return node == null || node.isNull() ? null : node.asLong();
    }

    private static Integer asInt(JsonNode node) {
        return node == null || node.isNull() ? null : node.asInt();
    }

    private static String summarizeError(String body) {
        if (!StringUtils.hasText(body)) {
            return "unknown";
        }
        return body.length() <= 280 ? body : body.substring(0, 280) + "...";
    }

    private static class RrfHolder {
        private final ChunkSearchHit hit;
        private double score;

        private RrfHolder(ChunkSearchHit hit, double score) {
            this.hit = hit;
            this.score = score;
        }
    }
}
