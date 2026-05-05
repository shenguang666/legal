package com.legal.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 知识库切片类型枚举。
 */
public enum KbChunkType {

    /** 普通分块，直接用于检索和大模型上下文。 */
    NORMAL("NORMAL"),
    /** 父分块，保存完整超长语义块，仅存数据库不进入 Elasticsearch。 */
    PARENT("PARENT"),
    /** 子分块，由父分块按自然边界拆分而来，用于 Elasticsearch 检索。 */
    CHILD("CHILD");

    @EnumValue
    @JsonValue
    private final String code;

    KbChunkType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
