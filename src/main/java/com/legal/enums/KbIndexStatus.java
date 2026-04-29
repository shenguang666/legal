package com.legal.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 知识库文档索引状态。
 */
public enum KbIndexStatus {

    /** 待索引：已入库但尚未进入索引流程。 */
    PENDING("PENDING"),
    /** 索引中：已生成/领取索引任务，正在构建 ES 索引。 */
    PROCESSING("PROCESSING"),
    /** 已完成：索引写入完成。 */
    COMPLETED("COMPLETED"),
    /** 失败：索引任务执行失败（可通过 outbox 重试）。 */
    FAILED("FAILED");

    @EnumValue
    @JsonValue
    private final String code;

    KbIndexStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
