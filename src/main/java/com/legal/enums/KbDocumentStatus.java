package com.legal.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 知识库文档状态。
 */
public enum KbDocumentStatus {

    /** 待处理：仅创建元数据或等待导入/索引准备。 */
    PENDING("PENDING"),
    /** 处理中：正在导入切片或已触发索引任务等待处理。 */
    PROCESSING("PROCESSING"),
    /** 可用：文档已完成索引，可参与检索。 */
    ACTIVE("ACTIVE"),
    /** 已删除：逻辑删除，不再参与检索/索引。 */
    DELETED("DELETED");

    @EnumValue
    @JsonValue
    private final String code;

    KbDocumentStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
