package com.legal.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 智能小法庭补证建议类型枚举。
 */
public enum CourtSuggestionType {

    /** 诉求缺少完整证据链支撑。 */
    CLAIM_NOT_CLOSED("CLAIM_NOT_CLOSED"),
    /** 合同、发票、流水或诉求之间金额不一致。 */
    AMOUNT_INCONSISTENT("AMOUNT_INCONSISTENT"),
    /** 履约、验收、通知等关键时间节点缺失。 */
    KEY_DATE_MISSING("KEY_DATE_MISSING"),
    /** 对方抗辩尚未被证据或观点有效反驳。 */
    DEFENSE_NOT_REBUTTED("DEFENSE_NOT_REBUTTED"),
    /** 规则外的自定义补证建议。 */
    CUSTOM("CUSTOM");

    @EnumValue
    @JsonValue
    private final String code;

    CourtSuggestionType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
