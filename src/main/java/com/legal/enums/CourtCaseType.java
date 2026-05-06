package com.legal.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 智能小法庭案件类型枚举，MVP 仅覆盖合同纠纷。
 */
public enum CourtCaseType {

    /** 合同纠纷案件类型。 */
    CONTRACT_DISPUTE("CONTRACT_DISPUTE");

    @EnumValue
    @JsonValue
    private final String code;

    CourtCaseType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
