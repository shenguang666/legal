package com.legal.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 索引任务出站状态。
 */
public enum KbOutboxStatus {

    /** 待执行：等待 worker 拉取处理。 */
    PENDING("PENDING"),
    /** 执行中：已被 worker 领取处理。 */
    PROCESSING("PROCESSING"),
    /** 已完成：执行成功。 */
    DONE("DONE"),
    /** 失败：执行失败，等待重试或人工处理。 */
    FAILED("FAILED");

    @EnumValue
    @JsonValue
    private final String code;

    KbOutboxStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
