package com.legal.contract.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.legal.enums.ContractReviewStatus;
import com.legal.enums.ContractRiskLevel;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 合同审阅实体类
 */
@Data
@TableName("contract_review")
public class ContractReviewEntity {

    /**
     * 合同审阅记录主键ID。
     */
    @TableId(value = "review_id", type = IdType.AUTO)
    private Long reviewId;

    /**
     * 租户ID，用于隔离不同企业的数据。
     */
    private Long tenantId;

    /**
     * 被审阅的知识库文档ID。
     */
    private Long documentId;

    /**
     * 审阅时绑定的文档版本号。
     */
    private Integer docVersion;

    /**
     * 文档归属用户ID。
     */
    private Long ownerUserId;

    /**
     * 触发本次审阅的用户ID。
     */
    private Long triggeredByUserId;

    /**
     * 审阅状态（待处理/处理中/完成/失败）。
     */
    private ContractReviewStatus status;

    /**
     * 当前审阅聚合出的总体风险等级。
     */
    private ContractRiskLevel riskLevel;

    /**
     * 命中的风险项总数。
     */
    private Integer riskCount;

    /**
     * 命中的规则数量。
     */
    private Integer hitRuleCount;

    /**
     * 本次审阅涉及的字段总数。
     */
    private Integer totalFieldCount;

    /**
     * 成功抽取出的字段数量。
     */
    private Integer extractedFieldCount;

    /**
     * 未抽取到的缺失字段数量。
     */
    private Integer missingFieldCount;

    /**
     * 面向前端展示的风险摘要或关键提示。
     */
    private String summaryText;

    /**
     * 审阅失败原因，便于用户重试和排查。
     */
    private String failureReason;

    /**
     * 后台开始执行审阅的时间。
     */
    private LocalDateTime startedAt;

    /**
     * 后台完成审阅的时间。
     */
    private LocalDateTime completedAt;

    /**
     * 记录创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 记录最近更新时间。
     */
    private LocalDateTime updatedAt;
}
