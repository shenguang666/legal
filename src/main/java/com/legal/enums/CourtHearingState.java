package com.legal.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 智能小法庭庭审轮次状态枚举。
 */
public enum CourtHearingState {

    /** 等待执行。 */
    PENDING("PENDING"),
    /** 正在执行。 */
    RUNNING("RUNNING"),
    /** 执行成功。 */
    SUCCEEDED("SUCCEEDED"),
    /** 执行失败。 */
    FAILED("FAILED"),
    /** 用户主动取消。 */
    CANCELLED("CANCELLED"),
    /** 因用户修改事实导致后续轮次过期。 */
    STALE("STALE");

    @EnumValue
    @JsonValue
    private final String code;

    CourtHearingState(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
