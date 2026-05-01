package com.legal.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ContractReviewStatus {

    PENDING("PENDING"),
    PROCESSING("PROCESSING"),
    COMPLETED("COMPLETED"),
    FAILED("FAILED");

    @EnumValue
    @JsonValue
    private final String code;

    ContractReviewStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
