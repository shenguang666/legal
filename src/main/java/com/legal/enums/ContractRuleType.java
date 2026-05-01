package com.legal.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ContractRuleType {

    REQUIRED_FIELD("REQUIRED_FIELD"),
    AMOUNT_CONSISTENCY("AMOUNT_CONSISTENCY"),
    DATE_ORDER("DATE_ORDER"),
    AMOUNT_THRESHOLD("AMOUNT_THRESHOLD"),
    LIABILITY_CONFLICT("LIABILITY_CONFLICT"),
    DOCUMENT_RETRIEVAL("DOCUMENT_RETRIEVAL");

    @EnumValue
    @JsonValue
    private final String code;

    ContractRuleType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
