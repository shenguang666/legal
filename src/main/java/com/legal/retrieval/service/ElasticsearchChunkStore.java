package com.legal.retrieval.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.legal.common.AppException;
import com.legal.config.ElasticsearchProperties;
import com.legal.enums.KbChunkType;
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
        upsertChunks(chunks, properties.getIndex().getKbChunks());
    }

    public void upsertChunks(List<ChunkIndexPayload> chunks, String indexName) {
        if (!isEnabled()) {
            return;
        }
        if (chunks == null || chunks.isEmpty()) {
            return;
        }
        ensureIndex(indexName);

        StringBuilder ndjson = new StringBuilder(chunks.size() * 300);
        for (ChunkIndexPayload chunk : chunks) {
            ndjson.append("{\"index\":{\"_index\":\"")
                    .append(indexName)
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
        deleteByDocument(tenantId, documentId, properties.getIndex().getKbChunks());
    }

    public void deleteByDocument(Long tenantId, Long documentId, String indexName) {
        if (!isEnabled()) {
            return;
        }
        ensureIndex(indexName);
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
                .uri("/" + indexName + "/_delete_by_query?conflicts=proceed")
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    public void deleteKbChunksIndex() {
        if (!isEnabled()) {
            return;
        }
        deleteIndex(properties.getIndex().getKbChunks());
    }

    public void deleteKnowledgeIndexes() {
        if (!isEnabled()) {
            return;
        }
        deleteIndex(properties.getIndex().getKbChunks());
        deleteIndex(properties.getIndex().getKbChunksMineru());
    }

    private void deleteIndex(String indexName) {
        try {
            restClient.delete()
                    .uri("/" + indexName)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                return;
            }
            throw new AppException(50013, 500, "删除 Elasticsearch 索引失败: " + summarizeError(ex.getResponseBodyAsString()));
        }
    }

    public List<ChunkSearchHit> hybridSearchRrf(Long tenantId,
                                                String question,
                                                List<Float> questionVector,
                                                double minVectorSimilarity,
                                                int topK,
                                                List<String> indexNames) {
        if (!isEnabled()) {
            return List.of();
        }
        List<String> targets = indexNames == null || indexNames.isEmpty() ? List.of(properties.getIndex().getKbChunks()) : indexNames;

        List<ChunkSearchHit> vectorHits = new ArrayList<>();
        List<ChunkSearchHit> bm25Hits = new ArrayList<>();
        for (String indexName : targets) {
            ensureIndex(indexName);
            vectorHits.addAll(vectorSearch(tenantId, questionVector, indexName)
                    .stream()
                    .filter(hit -> hit.getScore() >= minVectorSimilarity)
                    .toList());
            bm25Hits.addAll(bm25Search(tenantId, question, indexName));
        }
        return rrfMerge(vectorHits, bm25Hits, properties.getSearch().getRrfK(), topK);
    }

    /**
     * 仅向量检索（kNN）。返回的 score 为 Elasticsearch _score（通常对应 cosine similarity）。
     */
    public List<ChunkSearchHit> vectorSearch(Long tenantId, List<Float> questionVector, int topK) {
        if (!isEnabled()) {
            return List.of();
        }
        ensureIndex(properties.getIndex().getKbChunks());
        int safeTopK = Math.max(1, topK);
        return doVectorSearch(tenantId, questionVector, safeTopK, properties.getIndex().getKbChunks());
    }

    /**
     * 仅向量检索（kNN），支持指定目标索引。
     */
    public List<ChunkSearchHit> vectorSearch(Long tenantId,
                                             List<Float> questionVector,
                                             int topK,
                                             String indexName) {
        if (!isEnabled()) {
            return List.of();
        }
        ensureIndex(indexName);
        int safeTopK = Math.max(1, topK);
        return doVectorSearch(tenantId, questionVector, safeTopK, indexName);
    }

    private List<ChunkSearchHit> vectorSearch(Long tenantId, List<Float> questionVector, String indexName) {
        int vectorTopK = Math.max(1, properties.getSearch().getVectorTopK());
        return doVectorSearch(tenantId, questionVector, vectorTopK, indexName);
    }

    private List<ChunkSearchHit> doVectorSearch(Long tenantId,
                                                List<Float> questionVector,
                                                int vectorTopK,
                                                String indexName) {
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
                "_source", List.of("chunk_id", "document_id", "chunk_order", "source", "content", "chunk_type", "parent_chunk_id")
        );
        JsonNode response = restClient.post()
                .uri("/" + indexName + "/_search")
                .body(body)
                .retrieve()
                .body(JsonNode.class);
        return parseHits(response);
    }

    private List<ChunkSearchHit> bm25Search(Long tenantId, String question, String indexName) {
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
                "_source", List.of("chunk_id", "document_id", "chunk_order", "source", "content", "chunk_type", "parent_chunk_id")
        );
        JsonNode response = restClient.post()
                .uri("/" + indexName + "/_search")
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
            KbChunkType chunkType = resolveChunkType(source.path("chunk_type").asText(null));
            Long parentChunkId = asLong(source.path("parent_chunk_id"));
            double score = hitNode.path("_score").asDouble(0D);
            if (chunkId == null || documentId == null) {
                continue;
            }
            result.add(new ChunkSearchHit(chunkId, documentId, chunkOrder, sourceText, content, chunkType, parentChunkId, score));
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
                        holder.hit.getChunkType(),
                        holder.hit.getParentChunkId(),
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

    private void ensureIndex(String indexName) {
        try {
            restClient.put()
                    .uri("/" + indexName)
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
                        "properties", Map.ofEntries(
                                Map.entry("chunk_id", Map.of("type", "long")),
                                Map.entry("tenant_id", Map.of("type", "long")),
                                Map.entry("document_id", Map.of("type", "long")),
                                Map.entry("doc_version", Map.of("type", "integer")),
                                Map.entry("chunk_order", Map.of("type", "integer")),
                                Map.entry("chunk_type", Map.of("type", "keyword")),
                                Map.entry("parent_chunk_id", Map.of("type", "long")),
                                Map.entry("source", Map.of("type", "keyword")),
                                Map.entry("content", Map.of("type", "text")),
                                Map.entry("content_vector", Map.of(
                                        "type", "dense_vector",
                                        "dims", properties.getIndex().getVectorDims(),
                                        "index", true,
                                        "similarity", "cosine"
                                )),
                                Map.entry("updated_at", Map.of("type", "date"))
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
                + ",\"chunk_type\":\"" + escapeJson(resolveChunkTypeCode(chunk.getChunkType())) + "\""
                + (chunk.getParentChunkId() == null ? "" : ",\"parent_chunk_id\":" + chunk.getParentChunkId())
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

    private static KbChunkType resolveChunkType(String value) {
        if (!StringUtils.hasText(value)) {
            return KbChunkType.NORMAL;
        }
        try {
            return KbChunkType.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return KbChunkType.NORMAL;
        }
    }

    private static String resolveChunkTypeCode(KbChunkType chunkType) {
        return chunkType == null ? KbChunkType.NORMAL.getCode() : chunkType.getCode();
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
