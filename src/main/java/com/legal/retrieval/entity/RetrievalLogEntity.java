package com.legal.retrieval.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("retrieval_log")
public class RetrievalLogEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String traceId;
    private Long tenantId;
    private String queryText;
    private String hitChunkIds;
    private BigDecimal rerankScore;
    private String modelName;
    private Integer latencyMs;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getQueryText() {
        return queryText;
    }

    public void setQueryText(String queryText) {
        this.queryText = queryText;
    }

    public String getHitChunkIds() {
        return hitChunkIds;
    }

    public void setHitChunkIds(String hitChunkIds) {
        this.hitChunkIds = hitChunkIds;
    }

    public BigDecimal getRerankScore() {
        return rerankScore;
    }

    public void setRerankScore(BigDecimal rerankScore) {
        this.rerankScore = rerankScore;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public Integer getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Integer latencyMs) {
        this.latencyMs = latencyMs;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
