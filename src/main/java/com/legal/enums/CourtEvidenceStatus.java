package com.legal.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 智能小法庭证据状态枚举。
 */
public enum CourtEvidenceStatus {

    /** 证据有效，可用于庭审引用。 */
    ACTIVE("ACTIVE"),
    /** 证据因原文档删除或权限变化而失效。 */
    INVALID("INVALID"),
    /** 证据被用户从案件中撤回。 */
    REVOKED("REVOKED");

    @EnumValue
    @JsonValue
    private final String code;

    CourtEvidenceStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
