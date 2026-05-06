package com.legal.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 智能小法庭庭审发言角色枚举。
 */
public enum CourtArgumentSpeaker {

    /** 用户本人发言。 */
    USER("USER"),
    /** AI 法官发言。 */
    JUDGE("JUDGE"),
    /** AI 对方代理人发言。 */
    OPPONENT("OPPONENT"),
    /** AI 用户辅助律师发言。 */
    USER_ADVISOR("USER_ADVISOR"),
    /** 系统事件或系统提示。 */
    SYSTEM("SYSTEM");

    @EnumValue
    @JsonValue
    private final String code;

    CourtArgumentSpeaker(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
