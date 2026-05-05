package com.legal.retrieval.metrics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("rag_retrieval_metric_evaluation")
public class RagRetrievalMetricEvaluationEntity {

    /** 评估记录主键ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属 RAG 检索质量日汇总ID，历史数据允许为空。 */
    private Long dailySummaryId;
    /** 被评估的检索日志ID。 */
    private Long retrievalLogId;
    /** 租户ID。 */
    private Long tenantId;
    /** 原始检索问题文本。 */
    private String queryText;
    /** 原始命中的切片ID列表，JSON 数组格式。 */
    private String originalHitChunkIds;
    /** 最终进入大模型上下文的切片ID列表，JSON 数组格式。 */
    private String finalHitChunkIds;
    /** 评估发现的漏召回相关切片ID列表，JSON 数组格式。 */
    private String missedRelevantChunkIds;
    /** 原始命中且被判定相关的切片ID列表，JSON 数组格式。 */
    private String relevantOriginalChunkIds;
    /** 原始命中但被判定不相关的切片ID列表，JSON 数组格式。 */
    private String irrelevantOriginalChunkIds;
    /** 真阳性数量。 */
    private Integer truePositive;
    /** 假阴性数量。 */
    private Integer falseNegative;
    /** 假阳性数量。 */
    private Integer falsePositive;
    /** 召回率分数。 */
    private BigDecimal recallScore;
    /** 精确率分数。 */
    private BigDecimal precisionScore;
    /** 本次评估实际召回的候选切片数量。 */
    private Integer evaluatedCandidateCount;
    /** 本次评估配置的候选召回 TopN。 */
    private Integer evaluatedTopN;
    /** 执行判断的大模型名称。 */
    private String modelName;
    /** 评估提示词版本。 */
    private String promptVersion;
    /** 大模型判断摘要或原始结构化结果。 */
    private String judgeSummary;
    /** 大模型给出的可解释判断说明。 */
    private String explanation;
    /** 评估状态，取值 SUCCESS 或 FAILED。 */
    private String status;
    /** 失败时记录的错误信息。 */
    private String errorMessage;
    /** 评估版本，用于幂等和重跑。 */
    private String evaluationVersion;
    /** 评估完成时间。 */
    private LocalDateTime evaluatedAt;
    /** 记录创建时间。 */
    private LocalDateTime createdAt;
    /** 记录更新时间。 */
    private LocalDateTime updatedAt;
}
