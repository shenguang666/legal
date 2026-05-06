package com.legal.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 智能小法庭图谱事件类型枚举。
 */
public enum CourtGraphEventType {

    /** 新增或更新图谱节点事件。 */
    UPSERT_NODE("UPSERT_NODE"),
    /** 新增或更新图谱关系事件。 */
    UPSERT_RELATION("UPSERT_RELATION"),
    /** 更新图谱节点状态事件。 */
    UPDATE_NODE_STATUS("UPDATE_NODE_STATUS"),
    /** 标记图谱节点失效事件。 */
    INVALIDATE_NODE("INVALIDATE_NODE"),
    /** 删除某案件全部图谱子图事件。 */
    DELETE_CASE_GRAPH("DELETE_CASE_GRAPH");

    @EnumValue
    @JsonValue
    private final String code;

    CourtGraphEventType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
