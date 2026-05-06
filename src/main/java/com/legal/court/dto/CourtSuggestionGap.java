package com.legal.court.dto;

import com.legal.enums.CourtSuggestionSeverity;
import com.legal.enums.CourtSuggestionType;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 智能小法庭补证缺口规则结果。
 */
@Data
public class CourtSuggestionGap {

    /** 补证建议类型。 */
    private CourtSuggestionType type;
    /** 补证建议严重等级。 */
    private CourtSuggestionSeverity severity;
    /** 关联诉求业务ID。 */
    private String claimBusinessId;
    /** 关联抗辩业务ID。 */
    private String defenseBusinessId;
    /** 关联缺口或风险节点业务ID。 */
    private String gapOrRiskBusinessId;
    /** 缺失证据描述。 */
    private String missingDescription;
    /** 推荐补充材料列表。 */
    private List<String> recommendedMaterials = new ArrayList<>();
    /** 规则命中的节点业务ID列表。 */
    private List<String> relatedBusinessIds = new ArrayList<>();
    /** 规则解释依据。 */
    private String rationale;
    /** 原始规则查询属性。 */
    private Map<String, Object> rawProperties;
}
