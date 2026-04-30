package com.legal.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 记忆任务类型。
 */
public enum MemoryTaskType {

    SUMMARY_REFRESH("SUMMARY_REFRESH"),
    LONG_TERM_EXTRACT("LONG_TERM_EXTRACT"),
    USER_KNOWLEDGE_INDEX("USER_KNOWLEDGE_INDEX");

    @EnumValue
    @JsonValue
    private final String code;

    MemoryTaskType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
