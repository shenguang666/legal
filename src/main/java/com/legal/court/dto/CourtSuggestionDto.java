package com.legal.court.dto;

import com.legal.enums.CourtSuggestionSeverity;
import com.legal.enums.CourtSuggestionStatus;
import com.legal.enums.CourtSuggestionType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 智能小法庭补证建议 DTO。
 */
@Data
public class CourtSuggestionDto {

    /** 补证建议主键ID。 */
    private Long suggestionId;
    /** 租户ID。 */
    private Long tenantId;
    /** 案件ID。 */
    private Long caseId;
    /** 产生该建议的庭审轮次ID。 */
    private Long roundId;
    /** 建议类型。 */
    private CourtSuggestionType suggestionType;
    /** 严重等级。 */
    private CourtSuggestionSeverity severity;
    /** 建议状态。 */
    private CourtSuggestionStatus status;
    /** 受影响的诉求节点业务ID JSON 数组。 */
    private String affectedClaimIdsJson;
    /** 相关已有证据登记ID JSON 数组。 */
    private String affectedEvidenceIdsJson;
    /** 缺失证据或履约要素的简要描述。 */
    private String missingDescription;
    /** 推荐补充材料 JSON 数组。 */
    private String recommendedMaterialsJson;
    /** 可解释依据。 */
    private String rationale;
    /** 对模拟裁判观点的潜在影响说明。 */
    private String potentialImpact;
    /** 关联的 Gap/Risk 节点业务ID。 */
    private String graphGapBusinessId;
    /** 创建时间。 */
    private LocalDateTime createdAt;
    /** 更新时间。 */
    private LocalDateTime updatedAt;
}
