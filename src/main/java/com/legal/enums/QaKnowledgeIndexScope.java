package com.legal.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 智能问答知识库检索索引范围枚举。
 */
public enum QaKnowledgeIndexScope {

    /** 仅查询原生解析知识库索引。 */
    NATIVE_ONLY("NATIVE_ONLY"),
    /** 仅查询 MinerU 精准解析知识库索引。 */
    MINERU_ONLY("MINERU_ONLY"),
    /** 同时查询原生解析和 MinerU 精准解析知识库索引。 */
    BOTH("BOTH");

    @EnumValue
    @JsonValue
    private final String code;

    QaKnowledgeIndexScope(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
