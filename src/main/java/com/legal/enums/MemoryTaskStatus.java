package com.legal.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 记忆任务 outbox 状态。
 */
public enum MemoryTaskStatus {

    PENDING("PENDING"),
    PROCESSING("PROCESSING"),
    DONE("DONE"),
    FAILED("FAILED");

    @EnumValue
    @JsonValue
    private final String code;

    MemoryTaskStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
