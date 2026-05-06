package com.legal.court.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legal.common.AppException;
import com.legal.court.dto.CourtJudgeOutput;
import com.legal.court.dto.CourtJudgmentReportRequest;
import com.legal.court.entity.CourtArgumentEntity;
import com.legal.court.entity.CourtCaseEntity;
import com.legal.court.entity.CourtHearingMessageEntity;
import com.legal.court.entity.CourtJudgmentReportEntity;
import com.legal.court.mapper.CourtArgumentMapper;
import com.legal.court.mapper.CourtCaseMapper;
import com.legal.court.mapper.CourtHearingMessageMapper;
import com.legal.enums.CourtArgumentSpeaker;
import com.legal.court.mapper.CourtJudgmentReportMapper;
import com.legal.enums.CourtCaseStatus;
import com.legal.enums.CourtJudgmentReportStatus;
import com.legal.security.AuthPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 智能小法庭模拟裁判报告服务。
 */
@Slf4j
@Service
public class CourtJudgmentService {

    /** 报告展示和导出的固定合规水印。 */
    public static final String WATERMARK = "仅供模拟参考，不构成法律意见";

    private final CourtCaseService courtCaseService;
    private final CourtJudgmentReportMapper reportMapper;
    private final CourtCaseMapper courtCaseMapper;
    private final CourtArgumentMapper courtArgumentMapper;
    private final CourtHearingMessageMapper courtHearingMessageMapper;
    private final ObjectMapper objectMapper;

    public CourtJudgmentService(CourtCaseService courtCaseService,
                                CourtJudgmentReportMapper reportMapper,
                                CourtCaseMapper courtCaseMapper,
                                CourtArgumentMapper courtArgumentMapper,
                                CourtHearingMessageMapper courtHearingMessageMapper,
                                ObjectMapper objectMapper) {
        this.courtCaseService = courtCaseService;
        this.reportMapper = reportMapper;
        this.courtCaseMapper = courtCaseMapper;
        this.courtArgumentMapper = courtArgumentMapper;
        this.courtHearingMessageMapper = courtHearingMessageMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 基于已校验的 AI 法官输出生成结构化模拟裁判报告。
     */
    @Transactional
    public CourtJudgmentReportEntity generateReport(AuthPrincipal principal, Long caseId, CourtJudgmentReportRequest request) {
        CourtCaseEntity courtCase = courtCaseService.requireCase(principal, caseId);
        CourtArgumentEntity judgeArgument = null;
        CourtJudgeOutput output = request == null ? null : request.getJudgeOutput();
        if (output == null) {
            judgeArgument = latestJudgeArgument(principal.tenantId(), caseId);
            output = readJudgeOutput(judgeArgument);
        }
        validateJudgeOutput(output);
        CourtJudgmentReportEntity report = new CourtJudgmentReportEntity();
        report.setTenantId(principal.tenantId());
        report.setCaseId(caseId);
        report.setRoundId(resolveRoundId(request, judgeArgument));
        report.setStatus(CourtJudgmentReportStatus.GENERATED);
        report.setFocusIssuesJson(toJson(output.getFocusIssues()));
        report.setAcceptedFactsJson(toJson(output.getAcceptedFacts()));
        report.setRejectedFactsJson(toJson(output.getRejectedFacts()));
        report.setUnfavorableToPlaintiffJson(toJson(output.getUnfavorableToPartyA()));
        report.setUnfavorableToDefendantJson(toJson(output.getUnfavorableToPartyB()));
        report.setJudgmentPointsJson(toJson(buildJudgmentPoints(output)));
        report.setOpenQuestionsJson(toJson(output.getOpenQuestions()));
        report.setWatermark(WATERMARK);
        report.setCreatedAt(LocalDateTime.now());
        report.setUpdatedAt(LocalDateTime.now());
        reportMapper.insert(report);
        courtCase.setStatus(CourtCaseStatus.JUDGED);
        courtCase.setUpdatedAt(LocalDateTime.now());
        courtCaseMapper.updateById(courtCase);
        return report;
    }

    private CourtArgumentEntity latestJudgeArgument(Long tenantId, Long caseId) {
        CourtArgumentEntity argument = courtArgumentMapper.selectOne(new LambdaQueryWrapper<CourtArgumentEntity>()
                .eq(CourtArgumentEntity::getTenantId, tenantId)
                .eq(CourtArgumentEntity::getCaseId, caseId)
                .eq(CourtArgumentEntity::getSpeakerRole, CourtArgumentSpeaker.JUDGE)
                .orderByDesc(CourtArgumentEntity::getCreatedAt)
                .last("limit 1"));
        if (argument == null) {
            throw AppException.badRequest("暂无法官发言，无法生成模拟裁判报告");
        }
        return argument;
    }

    private CourtJudgeOutput readJudgeOutput(CourtArgumentEntity judgeArgument) {
        CourtHearingMessageEntity message = judgeArgument.getMessageId() == null ? null : courtHearingMessageMapper.selectById(judgeArgument.getMessageId());
        if (message == null || !StringUtils.hasText(message.getContent())) {
            throw AppException.badRequest("法官原始输出不存在，无法生成模拟裁判报告");
        }
        try {
            return objectMapper.readValue(message.getContent(), CourtJudgeOutput.class);
        } catch (Exception ex) {
            throw AppException.badRequest("法官原始输出格式异常，无法生成模拟裁判报告");
        }
    }

    private Long resolveRoundId(CourtJudgmentReportRequest request, CourtArgumentEntity judgeArgument) {
        if (request != null && request.getRoundId() != null) {
            return request.getRoundId();
        }
        return judgeArgument == null ? null : judgeArgument.getRoundId();
    }

    /**
     * 记录报告导出审计日志。
     */
    public void auditExport(AuthPrincipal principal, Long caseId, String exportFormat) {
        log.info("court.judgment.export tenantId={} userId={} caseId={} exportFormat={} timestamp={}",
                principal.tenantId(), principal.userId(), caseId, exportFormat, LocalDateTime.now());
    }

    private void validateJudgeOutput(CourtJudgeOutput output) {
        if (output == null) {
            throw AppException.badRequest("模拟裁判报告缺少法官输出");
        }
        if (output.getUnfavorableToPartyA() == null || output.getUnfavorableToPartyA().isEmpty()) {
            throw AppException.badRequest("模拟裁判报告缺少支持被告或对原告不利要点");
        }
        if (output.getUnfavorableToPartyB() == null || output.getUnfavorableToPartyB().isEmpty()) {
            throw AppException.badRequest("模拟裁判报告缺少支持原告或对被告不利要点");
        }
    }

    private List<String> buildJudgmentPoints(CourtJudgeOutput output) {
        if (output.getFocusIssues() == null || output.getFocusIssues().isEmpty()) {
            return List.of("证据不足，无法形成明确倾向性裁判观点");
        }
        return output.getFocusIssues().stream()
                .filter(StringUtils::hasText)
                .map(issue -> "围绕争议焦点【" + issue + "】形成模拟裁判分析，详见事实采信与双方不利要点。")
                .toList();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (Exception ex) {
            throw new IllegalStateException("模拟裁判报告 JSON 序列化失败", ex);
        }
    }
}
