package com.legal.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 智能小法庭案件证据角色枚举。
 */
public enum CourtEvidenceRole {

    /** 合同原文或合同附件，通常作为条款与义务来源。 */
    CONTRACT("CONTRACT"),
    /** 一般证据材料，例如验收单、聊天记录、付款凭证等。 */
    EVIDENCE("EVIDENCE"),
    /** 企业规则或风险规则依据。 */
    RULE("RULE");

    @EnumValue
    @JsonValue
    private final String code;

    CourtEvidenceRole(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
