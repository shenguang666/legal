package com.legal.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 智能小法庭补证建议状态枚举。
 */
public enum CourtSuggestionStatus {

    /** 建议仍然开放，等待用户补证或处理。 */
    OPEN("OPEN"),
    /** 建议已因补充证据或图谱更新而解除。 */
    RESOLVED("RESOLVED"),
    /** 建议被用户明确忽略。 */
    IGNORED("IGNORED");

    @EnumValue
    @JsonValue
    private final String code;

    CourtSuggestionStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
