package com.legal.court.dto;

import com.legal.enums.CourtHearingStage;
import com.legal.enums.CourtHearingState;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 智能小法庭庭审轮次 DTO。
 */
@Data
public class CourtHearingRoundDto {

    /** 庭审轮次主键ID。 */
    private Long roundId;
    /** 租户ID。 */
    private Long tenantId;
    /** 案件ID。 */
    private Long caseId;
    /** 案件内庭审轮次顺序号。 */
    private Integer roundNo;
    /** 庭审阶段。 */
    private CourtHearingStage stage;
    /** 轮次状态。 */
    private CourtHearingState state;
    /** 当前轮的尝试次数。 */
    private Integer attemptId;
    /** 乐观锁版本号。 */
    private Integer lockVersion;
    /** 本轮 LLM 累计消耗 token 数。 */
    private Integer totalTokens;
    /** 本轮失败原因或取消原因。 */
    private String failureReason;
    /** 本轮观点列表。 */
    private List<CourtArgumentDto> arguments;
    /** 本轮开始时间。 */
    private LocalDateTime startedAt;
    /** 本轮结束时间。 */
    private LocalDateTime endedAt;
    /** 创建时间。 */
    private LocalDateTime createdAt;
    /** 更新时间。 */
    private LocalDateTime updatedAt;
}
