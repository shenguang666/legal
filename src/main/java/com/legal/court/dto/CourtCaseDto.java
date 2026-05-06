package com.legal.court.dto;

import com.legal.enums.CourtCaseStatus;
import com.legal.enums.CourtCaseType;
import com.legal.enums.CourtUserSide;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 智能小法庭案件 DTO。
 */
@Data
public class CourtCaseDto {

    /** 案件主键ID。 */
    private Long caseId;
    /** 租户ID。 */
    private Long tenantId;
    /** 案件所属用户ID。 */
    private Long ownerUserId;
    /** 案件创建用户名快照。 */
    private String ownerUsername;
    /** 案件标题。 */
    private String title;
    /** 案件类型。 */
    private CourtCaseType caseType;
    /** 用户立场。 */
    private CourtUserSide userSide;
    /** 案件状态。 */
    private CourtCaseStatus status;
    /** 案件简要描述。 */
    private String caseSummary;
    /** 用户立场目标。 */
    private String userObjective;
    /** 案件要素是否已被用户确认。 */
    private Boolean factsConfirmed;
    /** 已完成的庭审轮次数量。 */
    private Integer totalRounds;
    /** 案件累计消耗 token 数。 */
    private Long totalTokens;
    /** 图谱投影状态。 */
    private String graphState;
    /** 案件当事人列表。 */
    private List<CourtPartyDto> parties;
    /** 案件证据列表。 */
    private List<CourtEvidenceDto> evidences;
    /** 创建时间。 */
    private LocalDateTime createdAt;
    /** 更新时间。 */
    private LocalDateTime updatedAt;
}
