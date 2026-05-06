package com.legal.court.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.legal.enums.CourtHearingStage;
import com.legal.enums.CourtHearingState;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 智能小法庭庭审轮次实体。
 */
@Data
@TableName("court_hearing_round")
public class CourtHearingRoundEntity {

    /** 庭审轮次主键ID。 */
    @TableId(value = "round_id", type = IdType.AUTO)
    private Long roundId;
    /** 租户ID。 */
    private Long tenantId;
    /** 案件ID。 */
    private Long caseId;
    /** 案件内庭审轮次顺序号，从1开始。 */
    private Integer roundNo;
    /** 庭审阶段。 */
    private CourtHearingStage stage;
    /** 轮次状态。 */
    private CourtHearingState state;
    /** 当前轮的尝试次数，重试时累加，用于幂等。 */
    private Integer attemptId;
    /** 乐观锁版本号。 */
    private Integer lockVersion;
    /** 本轮 LLM 累计消耗 token 数。 */
    private Integer totalTokens;
    /** 本轮失败原因或取消原因。 */
    private String failureReason;
    /** 本轮开始时间。 */
    private LocalDateTime startedAt;
    /** 本轮结束时间。 */
    private LocalDateTime endedAt;
    /** 创建时间。 */
    private LocalDateTime createdAt;
    /** 更新时间。 */
    private LocalDateTime updatedAt;
}
