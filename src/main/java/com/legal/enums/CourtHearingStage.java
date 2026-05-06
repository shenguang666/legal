package com.legal.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 智能小法庭庭审阶段枚举。
 */
public enum CourtHearingStage {

    /** 原告陈述阶段。 */
    OPENING_PLAINTIFF("OPENING_PLAINTIFF"),
    /** 被告答辩阶段。 */
    OPENING_DEFENDANT("OPENING_DEFENDANT"),
    /** 举证阶段。 */
    EVIDENCE("EVIDENCE"),
    /** 质证阶段。 */
    CROSS_EXAMINATION("CROSS_EXAMINATION"),
    /** 法庭辩论阶段。 */
    DEBATE("DEBATE"),
    /** 最后陈述阶段。 */
    CLOSING("CLOSING"),
    /** 模拟裁判意见阶段。 */
    JUDGMENT("JUDGMENT");

    @EnumValue
    @JsonValue
    private final String code;

    CourtHearingStage(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
