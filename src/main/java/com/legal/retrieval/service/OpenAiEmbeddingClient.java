package com.legal.retrieval.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.legal.common.AppException;
import com.legal.config.OpenAiEmbeddingProperties;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Component
public class OpenAiEmbeddingClient {

    private final OpenAiEmbeddingProperties properties;
    private final RestClient restClient;

    public OpenAiEmbeddingClient(OpenAiEmbeddingProperties properties) {
        this.properties = properties;
        this.restClient = buildClient(properties);
    }

    public List<Float> embed(String text) {
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw AppException.badRequest("请先配置 LEGAL_LLM_API_KEY 后再执行向量检索");
        }
        if (!StringUtils.hasText(properties.getModelName())) {
            throw AppException.badRequest("请先配置 LEGAL_LLM_EMBEDDING_MODEL 后再执行向量检索");
        }
        JsonNode response = restClient.post()
                .uri("/embeddings")
                .body(buildEmbeddingRequest(text))
                .retrieve()
                .body(JsonNode.class);
        JsonNode embeddingNode = response == null
                ? null
                : response.path("data").path(0).path("embedding");
        if (embeddingNode == null || !embeddingNode.isArray() || embeddingNode.isEmpty()) {
            throw new AppException(50010, 500, "Embedding 服务未返回有效向量");
        }
        List<Float> vector = new ArrayList<>(embeddingNode.size());
        for (JsonNode node : embeddingNode) {
            vector.add(node.floatValue());
        }
        return vector;
    }

    private RestClient buildClient(OpenAiEmbeddingProperties props) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Math.toIntExact(props.getTimeout().toMillis()));
        requestFactory.setReadTimeout(Math.toIntExact(props.getTimeout().toMillis()));
        return RestClient.builder()
                .baseUrl(props.getBaseUrl())
                .requestFactory(requestFactory)
                .defaultHeaders(headers -> {
                    headers.setBearerAuth(props.getApiKey());
                    headers.set("Content-Type", "application/json");
                })
                .build();
    }

    private Object buildEmbeddingRequest(String text) {
        String safeText = StringUtils.hasText(text) ? text : " ";
        if (properties.getDimensions() == null || properties.getDimensions() <= 0) {
            return new EmbeddingRequest(properties.getModelName(), safeText, null);
        }
        return new EmbeddingRequest(properties.getModelName(), safeText, properties.getDimensions());
    }

    private record EmbeddingRequest(String model, String input, Integer dimensions) {
    }
}
