package com.legal.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 用户角色。
 */
public enum UserRole {

    /** 系统管理员：可进行知识库管理等后台操作。 */
    ADMIN("ADMIN"),
    /** 普通用户：仅可进行问答与查看自身数据。 */
    USER("USER");

    @EnumValue
    @JsonValue
    private final String code;

    UserRole(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
