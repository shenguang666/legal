package com.legal.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ContractRiskSeverity {

    LOW("LOW"),
    MEDIUM("MEDIUM"),
    HIGH("HIGH");

    @EnumValue
    @JsonValue
    private final String code;

    ContractRiskSeverity(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
