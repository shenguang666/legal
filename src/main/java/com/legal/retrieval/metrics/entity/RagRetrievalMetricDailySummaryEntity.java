package com.legal.retrieval.metrics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("rag_retrieval_metric_daily_summary")
public class RagRetrievalMetricDailySummaryEntity {

    /** RAG 检索指标日汇总主键ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 租户ID。 */
    private Long tenantId;
    /** 指标业务日期。 */
    private LocalDate metricDate;
    /** 评估版本，用于区分提示词、TopN 和精筛口径。 */
    private String evaluationVersion;
    /** 当天使用过 RAG 检索的消息总数。 */
    private Long totalRagMessageCount;
    /** 当天未被过滤且进入评估流程的有效消息数。 */
    private Long validMessageCount;
    /** 当天被低质量 Query 过滤的消息数。 */
    private Long filteredMessageCount;
    /** 当天评估成功的消息数。 */
    private Long successCount;
    /** 当天评估失败的消息数。 */
    private Long failedCount;
    /** 当天跳过评估的消息数。 */
    private Long skippedCount;
    /** 当天成功样本的平均召回率。 */
    private BigDecimal averageRecall;
    /** 当天成功样本的平均精确率。 */
    private BigDecimal averagePrecision;
    /** 当天评估使用的粗召回 TopN 配置。 */
    private Integer candidateTopN;
    /** 日汇总任务状态。 */
    private String status;
    /** 日汇总任务失败或部分失败说明。 */
    private String errorMessage;
    /** 日汇总任务首次开始处理时间。 */
    private LocalDateTime startedAt;
    /** 日汇总任务最近完成时间。 */
    private LocalDateTime completedAt;
    /** 记录创建时间。 */
    private LocalDateTime createdAt;
    /** 记录更新时间。 */
    private LocalDateTime updatedAt;
}
