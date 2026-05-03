package com.legal.token.metrics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("token_usage_metric_top_user")
public class TokenUsageMetricTopUserEntity {

    /** Token 消耗 Top 用户明细主键ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属 Token 消耗日汇总ID。 */
    private Long dailySummaryId;
    /** 租户ID。 */
    private Long tenantId;
    /** 指标业务日期。 */
    private LocalDate metricDate;
    /** 用户ID。 */
    private Long userId;
    /** 登录用户名快照。 */
    private String username;
    /** 用户显示名称快照。 */
    private String displayName;
    /** 用户在该日 Token 消耗排行榜中的名次。 */
    private Integer rankNo;
    /** 该用户当天 Token 消耗总量。 */
    private Long tokenUsage;
    /** 该用户当天参与 Token 统计的助手消息数量。 */
    private Long messageCount;
    /** 该用户 Token 消耗占当日租户总消耗比例。 */
    private BigDecimal usageRatio;
    /** 统计版本，用于隔离不同统计口径。 */
    private String metricVersion;
    /** 记录创建时间。 */
    private LocalDateTime createdAt;
    /** 记录更新时间。 */
    private LocalDateTime updatedAt;
}
