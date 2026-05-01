package com.legal.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ContractFieldStatus {

    EXTRACTED("EXTRACTED"),
    MISSING("MISSING"),
    UNCERTAIN("UNCERTAIN");

    @EnumValue
    @JsonValue
    private final String code;

    ContractFieldStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
