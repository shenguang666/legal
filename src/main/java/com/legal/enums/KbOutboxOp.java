package com.legal.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 索引任务操作类型。
 */
public enum KbOutboxOp {

    /** 写入/更新索引：全量删除后按最新切片重建。 */
    UPSERT("UPSERT"),
    /** 删除索引：按 documentId 删除该文档的所有切片索引。 */
    DELETE("DELETE");

    @EnumValue
    @JsonValue
    private final String code;

    KbOutboxOp(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
