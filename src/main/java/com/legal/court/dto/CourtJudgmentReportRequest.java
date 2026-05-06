package com.legal.court.dto;

import lombok.Data;

/**
 * 智能小法庭模拟裁判报告生成请求。
 */
@Data
public class CourtJudgmentReportRequest {

    /** 触发报告生成的庭审轮次ID。 */
    private Long roundId;
    /** AI 法官结构化输出。 */
    private CourtJudgeOutput judgeOutput;
}
