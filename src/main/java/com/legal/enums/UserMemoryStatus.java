package com.legal.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 长期记忆条目状态。
 */
public enum UserMemoryStatus {

    CANDIDATE("CANDIDATE"),
    ACTIVE("ACTIVE"),
    REJECTED("REJECTED"),
    DELETED("DELETED");

    @EnumValue
    @JsonValue
    private final String code;

    UserMemoryStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
