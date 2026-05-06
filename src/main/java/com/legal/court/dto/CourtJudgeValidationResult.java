package com.legal.court.dto;

import lombok.Data;

/**
 * 智能小法庭 AI 法官输出校验结果。
 */
@Data
public class CourtJudgeValidationResult {

    /** 校验后的法官结构化输出。 */
    private CourtJudgeOutput output;
    /** 模型原始输出。 */
    private String rawText;
    /** 模型名称。 */
    private String modelName;
    /** 本次校验过程累计 token 消耗。 */
    private Integer tokenUsage;
    /** 是否通过双向不利点校验。 */
    private boolean valid;
    /** 实际重生成次数。 */
    private int retryCount;
    /** 校验失败原因。 */
    private String failureReason;
}
