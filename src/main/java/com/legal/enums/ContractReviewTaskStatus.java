package com.legal.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ContractReviewTaskStatus {

    PENDING("PENDING"),
    PROCESSING("PROCESSING"),
    DONE("DONE"),
    FAILED("FAILED");

    @EnumValue
    @JsonValue
    private final String code;

    ContractReviewTaskStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
