package com.legal.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 用户状态。
 */
public enum UserStatus {

    /** 启用：允许登录与访问系统功能。 */
    ACTIVE("ACTIVE"),
    /** 停用：禁止登录/鉴权不通过。 */
    INACTIVE("INACTIVE");

    @EnumValue
    @JsonValue
    private final String code;

    UserStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
