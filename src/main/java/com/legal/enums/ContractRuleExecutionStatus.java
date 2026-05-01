package com.legal.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ContractRuleExecutionStatus {

    HIT("HIT"),
    PASSED("PASSED"),
    SKIPPED("SKIPPED");

    @EnumValue
    @JsonValue
    private final String code;

    ContractRuleExecutionStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
