package com.legal.chat.cache;

import com.legal.common.JsonUtils;
import com.legal.retrieval.service.OpenAiEmbeddingClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * 语义相似度判定（Phase2）。
 *
 * <p>不引入向量数据库：由 AnswerCacheService 取候选池，再在内存做 cosine 相似度计算。</p>
 */
@Service
public class QuestionSimilarityService {

    private final OpenAiEmbeddingClient embeddingClient;

    public QuestionSimilarityService(OpenAiEmbeddingClient embeddingClient) {
        this.embeddingClient = embeddingClient;
    }

    public List<Float> embed(String text) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        return embeddingClient.embed(text);
    }

    public double cosine(List<Float> a, List<Float> b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty() || a.size() != b.size()) {
            return 0.0d;
        }
        double dot = 0.0d;
        double na = 0.0d;
        double nb = 0.0d;
        for (int i = 0; i < a.size(); i++) {
            double x = a.get(i);
            double y = b.get(i);
            dot += x * y;
            na += x * x;
            nb += y * y;
        }
        if (na <= 0 || nb <= 0) {
            return 0.0d;
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    /**
     * 将向量 JSON base64 化，避免 Redis value 中出现过长浮点数组导致可读性差。
     */
    public String encodeVector(List<Float> v) {
        if (v == null) {
            return null;
        }
        String json = JsonUtils.toJson(v);
        return Base64.getEncoder().encodeToString(json.getBytes());
    }

    public List<Float> decodeVector(String encoded) {
        if (!StringUtils.hasText(encoded)) {
            return List.of();
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(encoded);
            String json = new String(bytes);
            return JsonUtils.fromJsonList(json, Float.class);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
