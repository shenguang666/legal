package com.legal.retrieval.metrics.enums;

/**
 * RAG 检索指标日汇总任务状态枚举。
 */
public enum RagMetricDailyStatusEnum {

    /** 日汇总任务正在处理或等待后续批次继续处理。 */
    PROCESSING,
    /** 日汇总任务已处理完当天全部待扫描日志且无失败消息。 */
    COMPLETED,
    /** 日汇总任务已处理完当天全部待扫描日志但存在失败消息。 */
    PARTIAL_FAILED,
    /** 日汇总任务发生系统级失败，无法完成本轮统计。 */
    FAILED
}
