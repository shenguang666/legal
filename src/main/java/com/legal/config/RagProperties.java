package com.legal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "rag")
public class RagProperties {

    private boolean enabled = true;
    private int topK = 5;
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
