package com.legal.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 用户在智能小法庭案件中的立场枚举。
 */
public enum CourtUserSide {

    /** 用户作为原告参与模拟庭审。 */
    PLAINTIFF("PLAINTIFF"),
    /** 用户作为被告参与模拟庭审。 */
    DEFENDANT("DEFENDANT");

    @EnumValue
    @JsonValue
    private final String code;

    CourtUserSide(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
