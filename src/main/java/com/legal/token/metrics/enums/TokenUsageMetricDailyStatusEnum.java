package com.legal.token.metrics.enums;

/**
 * Token 消耗日汇总统计状态枚举。
 */
public enum TokenUsageMetricDailyStatusEnum {
    /** 统计任务正在处理该租户日期。 */
    PROCESSING,
    /** 统计任务已成功完成该租户日期。 */
    COMPLETED,
    /** 统计任务已完成但存在部分明细生成异常。 */
    PARTIAL_FAILED,
    /** 统计任务处理该租户日期失败。 */
    FAILED
}
