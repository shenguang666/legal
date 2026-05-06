package com.legal.court.dto;

import com.legal.enums.CourtCaseRole;
import com.legal.enums.CourtHearingStage;
import lombok.Data;

import java.util.List;

/**
 * 智能小法庭角色 Agent 调用请求。
 */
@Data
public class CourtRoleAgentRequest {

    /** 租户ID。 */
    private Long tenantId;
    /** 案件ID。 */
    private Long caseId;
    /** 庭审轮次ID。 */
    private Long roundId;
    /** 当前庭审阶段。 */
    private CourtHearingStage stage;
    /** 用户在案件中的立场。 */
    private CourtCaseRole userSide;
    /** 本轮问题或庭审指令。 */
    private String instruction;
    /** 案件事实摘要。 */
    private String caseSummary;
    /** 证据上下文，必须来自父级语义块。 */
    private String evidenceContext;
    /** 允许引用的证据白名单。 */
    private CourtEvidenceAllowedRefs allowedRefs;
    /** 历史庭审发言摘要。 */
    private List<String> hearingHistory;
}
