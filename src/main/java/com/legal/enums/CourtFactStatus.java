package com.legal.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 智能小法庭事实节点状态枚举。
 */
public enum CourtFactStatus {

    /** 用户或一方当事人主张的事实，尚未被证据支持。 */
    CLAIMED("CLAIMED"),
    /** 已存在证据支持的事实。 */
    EVIDENCED("EVIDENCED"),
    /** 同时存在支持与反驳证据的争议事实。 */
    DISPUTED("DISPUTED"),
    /** 被模拟法官在当前推演中采信的事实。 */
    ACCEPTED("ACCEPTED");

    @EnumValue
    @JsonValue
    private final String code;

    CourtFactStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
