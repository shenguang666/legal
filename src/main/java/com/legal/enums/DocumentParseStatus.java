package com.legal.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 文档解析状态枚举。
 */
public enum DocumentParseStatus {

    /** 待解析：文档已创建但尚未开始解析。 */
    PENDING("PENDING"),
    /** 解析中：文档正在本地解析或等待 MinerU 返回结果。 */
    PROCESSING("PROCESSING"),
    /** 解析完成：文档已生成可用切片。 */
    COMPLETED("COMPLETED"),
    /** 解析失败：文档解析过程出现不可用结果或超过重试次数。 */
    FAILED("FAILED");

    @EnumValue
    @JsonValue
    private final String code;

    DocumentParseStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
