package com.legal.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 智能小法庭图谱查询状态枚举。
 */
public enum CourtGraphState {

    /** 图谱已就绪。 */
    READY("READY"),
    /** 图谱仍有事件投影中。 */
    PROJECTING("PROJECTING"),
    /** Neo4j 不可用，已降级为 MySQL 数据。 */
    UNAVAILABLE("UNAVAILABLE");

    @JsonValue
    private final String code;

    CourtGraphState(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
