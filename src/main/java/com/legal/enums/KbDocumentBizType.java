package com.legal.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum KbDocumentBizType {

    KNOWLEDGE("KNOWLEDGE"),
    RISK_RULE("RISK_RULE"),
    TIANYAN_REVIEW("TIANYAN_REVIEW");

    @EnumValue
    private final String code;

    KbDocumentBizType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
