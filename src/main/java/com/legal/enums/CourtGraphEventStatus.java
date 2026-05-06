package com.legal.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 智能小法庭图谱事件投影状态枚举。
 */
public enum CourtGraphEventStatus {

    /** 等待投影到 Neo4j。 */
    PENDING("PENDING"),
    /** 已成功投影到 Neo4j。 */
    APPLIED("APPLIED"),
    /** 多次重试后仍失败，进入死信状态。 */
    DEAD("DEAD");

    @EnumValue
    @JsonValue
    private final String code;

    CourtGraphEventStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
