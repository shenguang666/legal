package com.legal.contract.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.legal.enums.ContractReviewTaskStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 合同审阅异步任务实体。
 */
@Data
@TableName("contract_review_task")
public class ContractReviewTaskEntity {

    /** 审阅异步任务主键ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 租户ID，用于按企业隔离任务。 */
    private Long tenantId;
    /** 关联的合同审阅记录ID。 */
    private Long reviewId;
    /** 关联的知识库文档ID。 */
    private Long documentId;
    /** 本次任务处理的文档版本号。 */
    private Integer docVersion;
    /** 异步任务当前状态。 */
    private ContractReviewTaskStatus status;
    /** 当前任务已重试次数。 */
    private Integer retryCount;
    /** 任务下次允许重试的时间。 */
    private LocalDateTime nextRetryAt;
    /** 最近一次失败原因。 */
    private String lastError;
    /** 任务创建时间。 */
    private LocalDateTime createdAt;
    /** 任务最近更新时间。 */
    private LocalDateTime updatedAt;
}
