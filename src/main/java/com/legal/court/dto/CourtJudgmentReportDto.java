package com.legal.court.dto;

import com.legal.enums.CourtJudgmentReportStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 智能小法庭模拟裁判报告 DTO。
 */
@Data
public class CourtJudgmentReportDto {

    /** 模拟裁判报告主键ID。 */
    private Long reportId;
    /** 租户ID。 */
    private Long tenantId;
    /** 案件ID。 */
    private Long caseId;
    /** 触发报告生成的庭审轮次ID。 */
    private Long roundId;
    /** 报告状态。 */
    private CourtJudgmentReportStatus status;
    /** 争议焦点 JSON 数组。 */
    private String focusIssuesJson;
    /** 事实认定 JSON 数组。 */
    private String acceptedFactsJson;
    /** 不予采信事实 JSON 数组。 */
    private String rejectedFactsJson;
    /** 对原告不利要点 JSON 数组。 */
    private String unfavorableToPlaintiffJson;
    /** 对被告不利要点 JSON 数组。 */
    private String unfavorableToDefendantJson;
    /** 模拟裁判观点 JSON 数组。 */
    private String judgmentPointsJson;
    /** 法官提出的尚未澄清问题 JSON 数组。 */
    private String openQuestionsJson;
    /** 导出/展示时附加的合规水印文案。 */
    private String watermark;
    /** 创建时间。 */
    private LocalDateTime createdAt;
    /** 更新时间。 */
    private LocalDateTime updatedAt;
}
