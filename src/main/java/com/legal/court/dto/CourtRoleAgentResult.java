package com.legal.court.dto;

import com.legal.enums.CourtArgumentSpeaker;
import lombok.Data;

/**
 * 智能小法庭角色 Agent 调用结果。
 */
@Data
public class CourtRoleAgentResult {

    /** 发言角色。 */
    private CourtArgumentSpeaker speaker;
    /** 模型名称。 */
    private String modelName;
    /** 模型原始输出。 */
    private String rawText;
    /** 本次调用 token 消耗。 */
    private Integer tokenUsage;
}
