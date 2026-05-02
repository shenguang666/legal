package com.legal.retrieval.metrics.enums;

/**
 * RAG 检索指标单条消息评估状态枚举。
 */
public enum RagMetricEvaluationStatusEnum {

    /** 评估成功，Recall 和 Precision 指标可纳入质量均值。 */
    SUCCESS,
    /** 评估失败，例如大模型空响应、解析失败或候选召回异常。 */
    FAILED,
    /** 跳过评估，例如 Query 被低质量规则过滤。 */
    SKIPPED
}
