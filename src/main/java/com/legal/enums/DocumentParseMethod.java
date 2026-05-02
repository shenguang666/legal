package com.legal.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 文档解析方式枚举。
 */
public enum DocumentParseMethod {

    /** 原生解析：使用本地 Tika 抽取文本并按本地策略切片。 */
    NATIVE("NATIVE"),
    /** MinerU 精准解析：使用 MinerU v4 精准解析获取 Markdown/结构化内容。 */
    MINERU_PRECISE("MINERU_PRECISE");

    @EnumValue
    @JsonValue
    private final String code;

    DocumentParseMethod(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
