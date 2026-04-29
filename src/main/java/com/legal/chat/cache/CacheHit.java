package com.legal.chat.cache;

/**
 * 缓存命中结果。
 */
public class CacheHit {

    private final AnswerCachePayload payload;
    private final String scope; // USER / TENANT
    private final String type;  // EXACT / SEMANTIC
    private final Double similarityScore; // 语义命中时可用

    public CacheHit(AnswerCachePayload payload, String scope, String type, Double similarityScore) {
        this.payload = payload;
        this.scope = scope;
        this.type = type;
        this.similarityScore = similarityScore;
    }

    public AnswerCachePayload getPayload() {
        return payload;
    }

    public String getScope() {
        return scope;
    }

    public String getType() {
        return type;
    }

    public Double getSimilarityScore() {
        return similarityScore;
    }
}
