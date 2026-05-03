package com.legal.token.metrics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("token_usage_metric_daily_summary")
public class TokenUsageMetricDailySummaryEntity {

    /** Token 消耗日汇总主键ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 租户ID。 */
    private Long tenantId;
    /** 指标业务日期。 */
    private LocalDate metricDate;
    /** 统计版本，用于隔离不同统计口径。 */
    private String metricVersion;
    /** 当天统计到的 Token 总消耗。 */
    private Long totalTokenUsage;
    /** 当天参与 Token 统计的助手消息数量。 */
    private Long messageCount;
    /** 当天产生 Token 消耗的活跃用户数量。 */
    private Long activeUserCount;
    /** 当天平均每条助手消息消耗的 Token 数。 */
    private BigDecimal averageTokensPerMessage;
    /** 本次日汇总保留的 Top 用户数量。 */
    private Integer topUserLimit;
    /** 实际写入的 Top 用户明细数量。 */
    private Integer topUserCount;
    /** 日汇总任务状态。 */
    private String status;
    /** 日汇总任务失败或部分失败说明。 */
    private String errorMessage;
    /** 日汇总任务失败重试次数。 */
    private Integer retryCount;
    /** 日汇总任务首次开始处理时间。 */
    private LocalDateTime startedAt;
    /** 日汇总任务最近完成时间。 */
    private LocalDateTime completedAt;
    /** 记录创建时间。 */
    private LocalDateTime createdAt;
    /** 记录更新时间。 */
    private LocalDateTime updatedAt;
}
