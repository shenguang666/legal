package com.legal.retrieval.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 检索日志表实体。
 */
@Data
@TableName("retrieval_log")
public class RetrievalLogEntity {

    /** 日志主键ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 链路追踪ID。 */
    private String traceId;
    /** 租户ID。 */
    private Long tenantId;
    /** 检索查询文本。 */
    private String queryText;
    /** 命中的切片ID列表。 */
    private String hitChunkIds;
    /** 重排得分。 */
    private BigDecimal rerankScore;
    /** 使用的模型名称。 */
    private String modelName;
    /** 检索耗时（毫秒）。 */
    private Integer latencyMs;
    /** RAG 指标扫描状态。 */
    private String ragMetricScanStatus;
    /** RAG 指标最近扫描完成时间。 */
    private LocalDateTime ragMetricScannedAt;
    /** RAG 指标日汇总记录ID。 */
    private Long ragMetricSummaryId;
    /** RAG 指标扫描失败后的重试次数。 */
    private Integer ragMetricRetryCount;
    /** RAG 指标扫描失败或过滤原因。 */
    private String ragMetricErrorMessage;
    /** 创建时间。 */
    private LocalDateTime createdAt;
}
