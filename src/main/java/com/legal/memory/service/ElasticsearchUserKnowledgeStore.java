package com.legal.memory.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.legal.common.AppException;
import com.legal.config.ElasticsearchProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ElasticsearchUserKnowledgeStore {

    private final ElasticsearchProperties properties;
    private final RestClient restClient;

    public ElasticsearchUserKnowledgeStore(ElasticsearchProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getPrimaryUri())
                .defaultHeaders(headers -> {
                    if (StringUtils.hasText(properties.getUsername())) {
                        headers.setBasicAuth(properties.getUsername(), properties.getPassword());
                    }
                    headers.setContentType(MediaType.APPLICATION_JSON);
                })
                .build();
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public void upsert(Long knowledgeId, Long tenantId, Long userId, String source, String content, List<Float> vector) {
        if (!isEnabled()) return;
        ensureIndex();
        String vec = vector.stream().map(v -> String.format(Locale.ROOT, "%.8f", v)).collect(Collectors.joining(","));
        String body = "{\"knowledge_id\":" + knowledgeId
                + ",\"tenant_id\":" + tenantId
                + ",\"user_id\":" + userId
                + ",\"source\":\"" + escape(source) + "\""
                + ",\"content\":\"" + escape(content) + "\""
                + ",\"content_vector\":[" + vec + "]"
                + ",\"updated_at\":\"" + LocalDateTime.now() + "\"}";
        restClient.put().uri("/" + properties.getIndex().getUserKnowledge() + "/_doc/" + knowledgeId)
                .contentType(MediaType.APPLICATION_JSON).body(body.getBytes(StandardCharsets.UTF_8)).retrieve().toBodilessEntity();
    }

    public List<String> search(Long tenantId, Long userId, List<Float> queryVector, int topK) {
        if (!isEnabled() || queryVector == null || queryVector.isEmpty()) return List.of();
        ensureIndex();
        Map<String, Object> body = Map.of(
                "size", Math.max(1, topK),
                "knn", Map.of(
                        "field", "content_vector",
                        "query_vector", queryVector,
                        "k", Math.max(1, topK),
                        "num_candidates", Math.max(50, topK * 2),
                        "filter", Map.of("bool", Map.of("filter", List.of(
                                Map.of("term", Map.of("tenant_id", tenantId)),
                                Map.of("term", Map.of("user_id", userId))
                        )))),
                "_source", List.of("source", "content"));
        JsonNode response = restClient.post().uri("/" + properties.getIndex().getUserKnowledge() + "/_search").body(body).retrieve().body(JsonNode.class);
        List<String> result = new ArrayList<>();
        for (JsonNode hit : response.path("hits").path("hits")) {
            JsonNode src = hit.path("_source");
            String source = src.path("source").asText("用户知识");
            String content = src.path("content").asText("");
            if (StringUtils.hasText(content)) result.add(source + "\n" + content);
        }
        return result;
    }

    private void ensureIndex() {
        try {
            restClient.put().uri("/" + properties.getIndex().getUserKnowledge()).contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("mappings", Map.of("properties", Map.of(
                            "knowledge_id", Map.of("type", "long"),
                            "tenant_id", Map.of("type", "long"),
                            "user_id", Map.of("type", "long"),
                            "source", Map.of("type", "keyword"),
                            "content", Map.of("type", "text"),
                            "content_vector", Map.of("type", "dense_vector", "dims", properties.getIndex().getVectorDims(), "index", true, "similarity", "cosine"),
                            "updated_at", Map.of("type", "date"))))).retrieve().toBodilessEntity();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 400 && ex.getResponseBodyAsString().contains("resource_already_exists_exception")) return;
            throw new AppException(50012, 500, "初始化用户知识 ES 索引失败");
        }
    }

    private String escape(String v) {
        return v == null ? "" : v.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
