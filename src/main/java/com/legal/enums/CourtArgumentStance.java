package com.legal.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 智能小法庭庭审观点立场枚举。
 */
public enum CourtArgumentStance {

    /** 支持某一方主张或诉求。 */
    SUPPORT("SUPPORT"),
    /** 反驳某一方主张或诉求。 */
    REBUT("REBUT"),
    /** 中立陈述或程序性发言。 */
    NEUTRAL("NEUTRAL"),
    /** 缺少证据支撑，暂列为待证明。 */
    PENDING_PROOF("PENDING_PROOF");

    @EnumValue
    @JsonValue
    private final String code;

    CourtArgumentStance(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
