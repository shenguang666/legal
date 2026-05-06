package com.legal.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 智能小法庭案件参与立场枚举，表示某一方在案件中的诉讼角色。
 */
public enum CourtCaseRole {

    /** 原告立场。 */
    PLAINTIFF("PLAINTIFF"),
    /** 被告立场。 */
    DEFENDANT("DEFENDANT");

    @EnumValue
    @JsonValue
    private final String code;

    CourtCaseRole(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
