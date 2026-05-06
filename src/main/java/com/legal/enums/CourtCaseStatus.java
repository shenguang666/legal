package com.legal.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 智能小法庭案件状态枚举。
 */
public enum CourtCaseStatus {

    /** 草稿，仅创建未确认要素。 */
    DRAFT("DRAFT"),
    /** 要素已被用户确认，可开庭。 */
    READY("READY"),
    /** 庭审进行中。 */
    HEARING("HEARING"),
    /** 已生成模拟裁判报告。 */
    JUDGED("JUDGED"),
    /** 已归档。 */
    ARCHIVED("ARCHIVED"),
    /** 已软删除。 */
    DELETED("DELETED");

    @EnumValue
    @JsonValue
    private final String code;

    CourtCaseStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
