package com.legal.retrieval.metrics.enums;

/**
 * retrieval_log 的 RAG 指标扫描状态枚举。
 */
public enum RagMetricScanStatusEnum {

    /** 待 RAG 指标定时任务扫描。 */
    PENDING,
    /** 当前日志正在被 RAG 指标任务处理。 */
    PROCESSING,
    /** 当前日志已成功生成消息级评估结果。 */
    SUCCESS,
    /** 当前日志评估失败，后续可按重试策略再次处理。 */
    FAILED,
    /** 当前日志命中过滤规则并被跳过，不进入大模型评估；数据库值继续使用 FILTERED。 */
    FILTERED
}
