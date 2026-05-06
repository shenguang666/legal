package com.legal.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 智能小法庭案件当事人角色枚举。
 */
public enum CourtPartyRole {

    /** 原告。 */
    PLAINTIFF("PLAINTIFF"),
    /** 被告。 */
    DEFENDANT("DEFENDANT"),
    /** 第三人。 */
    THIRD_PARTY("THIRD_PARTY");

    @EnumValue
    @JsonValue
    private final String code;

    CourtPartyRole(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
