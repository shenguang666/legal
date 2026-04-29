package com.legal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "rag")
public class RagProperties {

    private boolean enabled = true;
    private int topK = 5;
    /**
     * 向量检索最小相似度阈值（cosine similarity）。
     * 
     * <p>当使用 Elasticsearch dense_vector + cosine 相似度时，_score 通常对应 cosineSimilarity。
     * 这里用于过滤低质量召回；如果过滤后条数不足，不会强行补齐。</p>
     */
    private double minVectorSimilarity = 0.75d;
    private int maxMemoryMessages = 12;
    private String emptyHitWarning = "当前知识库暂无直接相关内容，以下回答仅基于通用法律知识整理，请谨慎核对。";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getTopK() {
        return topK;
    }

    public void setTopK(int topK) {
        this.topK = topK;
    }

    public double getMinVectorSimilarity() {
        return minVectorSimilarity;
    }

    public void setMinVectorSimilarity(double minVectorSimilarity) {
        this.minVectorSimilarity = minVectorSimilarity;
    }

    public int getMaxMemoryMessages() {
        return maxMemoryMessages;
    }

    public void setMaxMemoryMessages(int maxMemoryMessages) {
        this.maxMemoryMessages = maxMemoryMessages;
    }

    public String getEmptyHitWarning() {
        return emptyHitWarning;
    }

    public void setEmptyHitWarning(String emptyHitWarning) {
        this.emptyHitWarning = emptyHitWarning;
    }
}
