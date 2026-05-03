package com.legal.token.metrics.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class TokenUsageMetricDtos {

    private TokenUsageMetricDtos() {
    }

    @Data
    public static class Summary {
        /** 日期范围内 Token 消耗总量。 */
        private long totalTokenUsage;
        /** 日期范围内参与统计的助手消息总数。 */
        private long messageCount;
        /** 日期范围内日活跃用户数累计值。 */
        private long activeUserCount;
        /** 日期范围内平均每条助手消息消耗的 Token 数。 */
        private BigDecimal averageTokensPerMessage;
        /** 当前 Token 指标统计版本。 */
        private String metricVersion;
        /** 日期范围内每日趋势点。 */
        private List<TrendPoint> trends;
        /** 日期范围内日汇总列表。 */
        private List<DailySummary> dailySummaries;
    }

    @Data
    public static class TrendPoint {
        /** 指标业务日期。 */
        private LocalDate date;
        /** 当天 Token 消耗总量。 */
        private long totalTokenUsage;
        /** 当天参与统计的助手消息数量。 */
        private long messageCount;
        /** 当天活跃用户数量。 */
        private long activeUserCount;
        /** 当天平均每条助手消息消耗的 Token 数。 */
        private BigDecimal averageTokensPerMessage;
        /** 当天日汇总状态。 */
        private String status;
        /** 当天日汇总主键ID。 */
        private Long dailySummaryId;
    }

    @Data
    public static class DailySummary {
        /** Token 消耗日汇总主键ID。 */
        private Long id;
        /** 指标业务日期。 */
        private LocalDate metricDate;
        /** 统计版本。 */
        private String metricVersion;
        /** 当天 Token 消耗总量。 */
        private long totalTokenUsage;
        /** 当天参与统计的助手消息数量。 */
        private long messageCount;
        /** 当天活跃用户数量。 */
        private long activeUserCount;
        /** 当天平均每条助手消息消耗的 Token 数。 */
        private BigDecimal averageTokensPerMessage;
        /** 本次日汇总配置的 Top 用户数量。 */
        private Integer topUserLimit;
        /** 实际写入的 Top 用户数量。 */
        private Integer topUserCount;
        /** 日汇总状态。 */
        private String status;
        /** 失败或部分失败说明。 */
        private String errorMessage;
        /** 首次开始处理时间。 */
        private LocalDateTime startedAt;
        /** 最近完成处理时间。 */
        private LocalDateTime completedAt;
    }

    @Data
    public static class DailySummaryPage {
        /** 总记录数。 */
        private long total;
        /** 当前页码。 */
        private int pageNo;
        /** 每页大小。 */
        private int pageSize;
        /** 当前页日汇总记录。 */
        private List<DailySummary> items;
    }

    @Data
    public static class TopUser {
        /** Top 用户明细主键ID。 */
        private Long id;
        /** 所属日汇总ID。 */
        private Long dailySummaryId;
        /** 指标业务日期。 */
        private LocalDate metricDate;
        /** 用户ID。 */
        private Long userId;
        /** 登录用户名快照。 */
        private String username;
        /** 用户显示名称快照。 */
        private String displayName;
        /** 排名。 */
        private Integer rankNo;
        /** 用户 Token 消耗总量。 */
        private long tokenUsage;
        /** 用户参与统计的助手消息数量。 */
        private long messageCount;
        /** 用户 Token 消耗占当天总量比例。 */
        private BigDecimal usageRatio;
    }

    @Data
    public static class RunResult {
        /** 实际处理的单个指标业务日期；日期范围统计时为空。 */
        private LocalDate metricDate;
        /** 手动统计请求的开始日期。 */
        private LocalDate startDate;
        /** 手动统计请求的结束日期。 */
        private LocalDate endDate;
        /** 本次选中的租户日期任务数量。 */
        private int selected;
        /** 本次成功统计的租户日期任务数量。 */
        private int success;
        /** 本次统计失败的租户日期任务数量。 */
        private int failed;
        /** 本次跳过的租户日期任务数量。 */
        private int skipped;
        /** 本次被判定为已完整统计、无需重复统计的日期数量。 */
        private int alreadyProcessedDates;
        /** 是否因为目标日期已完整统计而跳过本次统计。 */
        private boolean alreadyProcessed;
        /** 本次运行结果提示，用于前端展示。 */
        private String message;
        /** 本次未执行统计时的跳过原因。 */
        private String skippedReason;
    }

    @Data
    public static class AggregatedUsage {
        /** 聚合后的 Token 消耗总量。 */
        private Long totalTokenUsage;
        /** 聚合后的助手消息数量。 */
        private Long messageCount;
        /** 聚合后的活跃用户数量。 */
        private Long activeUserCount;
    }

    @Data
    public static class TopUserUsage {
        /** 用户ID。 */
        private Long userId;
        /** 登录用户名快照。 */
        private String username;
        /** 用户显示名称快照。 */
        private String displayName;
        /** 用户 Token 消耗总量。 */
        private Long tokenUsage;
        /** 用户参与统计的助手消息数量。 */
        private Long messageCount;
    }
}
