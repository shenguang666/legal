package com.legal.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * MinerU 图片图生文描述状态枚举。
 */
public enum ImageCaptionStatus {

    /** 图片描述待处理。 */
    PENDING("PENDING"),
    /** 图片描述生成成功。 */
    SUCCESS("SUCCESS"),
    /** 图片描述生成失败。 */
    FAILED("FAILED"),
    /** 图片描述被跳过。 */
    SKIPPED("SKIPPED");

    /** 数据库存储的状态编码。 */
    @EnumValue
    private final String code;

    ImageCaptionStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
