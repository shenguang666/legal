package com.legal.court.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.legal.common.ApiResponse;
import com.legal.court.agent.CourtOrchestrator;
import com.legal.court.dto.CourtCaseCreateRequest;
import com.legal.court.dto.CourtCaseUpdateRequest;
import com.legal.court.dto.CourtJudgmentReportRequest;
import com.legal.court.entity.CourtCaseEntity;
import com.legal.court.entity.CourtHearingRoundEntity;
import com.legal.court.entity.CourtJudgmentReportEntity;
import com.legal.court.service.CourtCaseService;
import com.legal.court.service.CourtJudgmentService;
import com.legal.security.AuthContextHolder;
import com.legal.security.AuthPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 智能小法庭案件与庭审 Controller。
 */
@Validated
@RestController
@RequestMapping("/api/smart-court/cases")
public class SmartCourtController {

    private final CourtCaseService courtCaseService;
    private final CourtOrchestrator courtOrchestrator;
    private final CourtJudgmentService courtJudgmentService;

    public SmartCourtController(CourtCaseService courtCaseService,
                                CourtOrchestrator courtOrchestrator,
                                CourtJudgmentService courtJudgmentService) {
        this.courtCaseService = courtCaseService;
        this.courtOrchestrator = courtOrchestrator;
        this.courtJudgmentService = courtJudgmentService;
    }

    @GetMapping
    public ApiResponse<Page<CourtCaseEntity>> page(@RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
                                                   @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(courtCaseService.pageCases(principal, pageNo, pageSize));
    }

    @PostMapping
    public ApiResponse<CourtCaseEntity> create(@RequestBody CourtCaseCreateRequest request) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(courtCaseService.createCase(principal, request));
    }

    @GetMapping("/{caseId}")
    public ApiResponse<CourtCaseEntity> detail(@PathVariable Long caseId) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(courtCaseService.requireCase(principal, caseId));
    }

    @PutMapping("/{caseId}")
    public ApiResponse<CourtCaseEntity> update(@PathVariable Long caseId, @RequestBody CourtCaseUpdateRequest request) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(courtCaseService.updateCase(principal, caseId, request));
    }

    @PostMapping("/{caseId}/confirm-facts")
    public ApiResponse<CourtCaseEntity> confirmFacts(@PathVariable Long caseId) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(courtCaseService.confirmFacts(principal, caseId));
    }

    @PostMapping("/{caseId}/start-hearing")
    public ApiResponse<CourtCaseEntity> startHearing(@PathVariable Long caseId) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(courtCaseService.startHearing(principal, caseId));
    }

    @PostMapping("/{caseId}/rounds/{roundId}/start")
    public ApiResponse<CourtHearingRoundEntity> startRound(@PathVariable Long caseId, @PathVariable Long roundId) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(courtOrchestrator.startRound(principal.tenantId(), caseId, roundId));
    }

    @PostMapping("/{caseId}/rounds/{roundId}/stop")
    public ApiResponse<Void> stopRound(@PathVariable Long caseId, @PathVariable Long roundId) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        courtOrchestrator.stopRound(principal.tenantId(), caseId, roundId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{caseId}/archive")
    public ApiResponse<Void> archive(@PathVariable Long caseId) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        courtCaseService.archiveCase(principal, caseId);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{caseId}")
    public ApiResponse<Void> delete(@PathVariable Long caseId) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        courtCaseService.softDeleteCase(principal, caseId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{caseId}/judgment-report")
    public ApiResponse<CourtJudgmentReportEntity> generateReport(@PathVariable Long caseId,
                                                                 @RequestBody CourtJudgmentReportRequest request) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(courtJudgmentService.generateReport(principal, caseId, request));
    }

    @PostMapping("/{caseId}/judgment-report/export-audit")
    public ApiResponse<Void> auditReportExport(@PathVariable Long caseId,
                                               @RequestParam("exportFormat") String exportFormat) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        courtJudgmentService.auditExport(principal, caseId, exportFormat);
        return ApiResponse.ok(null);
    }
}
