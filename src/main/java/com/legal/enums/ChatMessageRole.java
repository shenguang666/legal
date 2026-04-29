package com.legal.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 聊天消息角色。
 */
public enum ChatMessageRole {

    /** 用户发言。 */
    USER("user"),
    /** 助手发言（模型/缓存命中结果）。 */
    ASSISTANT("assistant");

    @EnumValue
    @JsonValue
    private final String code;

    ChatMessageRole(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
