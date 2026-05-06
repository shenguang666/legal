package com.legal.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 智能小法庭补证建议严重等级枚举。
 */
public enum CourtSuggestionSeverity {

    /** 高严重度，可能直接影响核心诉求成立。 */
    HIGH("HIGH"),
    /** 中严重度，会削弱诉求或抗辩的证明力。 */
    MEDIUM("MEDIUM"),
    /** 低严重度，主要影响表达完整性或辅助证明。 */
    LOW("LOW");

    @EnumValue
    @JsonValue
    private final String code;

    CourtSuggestionSeverity(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
