package com.legal.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 智能小法庭模拟裁判报告状态枚举。
 */
public enum CourtJudgmentReportStatus {

    /** 报告已生成。 */
    GENERATED("GENERATED"),
    /** 报告正在重生成。 */
    REGENERATING("REGENERATING"),
    /** 报告因案件事实或证据变化而失效。 */
    INVALID("INVALID");

    @EnumValue
    @JsonValue
    private final String code;

    CourtJudgmentReportStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
