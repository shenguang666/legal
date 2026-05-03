package com.legal.token.metrics.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.legal.common.ApiResponse;
import com.legal.security.AuthContextHolder;
import com.legal.security.AuthPrincipal;
import com.legal.token.metrics.dto.TokenUsageMetricDtos;
import com.legal.token.metrics.service.TokenUsageMetricService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@SaCheckRole("ADMIN")
@Validated
@RestController
@RequestMapping("/api/token-usage-metrics")
public class TokenUsageMetricController {

    private final TokenUsageMetricService metricService;

    public TokenUsageMetricController(TokenUsageMetricService metricService) {
        this.metricService = metricService;
    }

    @GetMapping("/summary")
    public ApiResponse<TokenUsageMetricDtos.Summary> summary(@RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                             @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(metricService.summary(principal.tenantId(), startDate, endDate));
    }

    @GetMapping("/daily-summaries")
    public ApiResponse<TokenUsageMetricDtos.DailySummaryPage> dailySummaries(@RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                                             @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                                             @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
                                                                             @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(metricService.dailySummaries(principal.tenantId(), startDate, endDate, pageNo, pageSize));
    }

    @GetMapping("/top-users")
    public ApiResponse<List<TokenUsageMetricDtos.TopUser>> topUsers(@RequestParam("dailySummaryId") Long dailySummaryId) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(metricService.topUsers(principal.tenantId(), dailySummaryId));
    }

    @PostMapping("/run")
    public ApiResponse<TokenUsageMetricDtos.RunResult> run(@RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                                           @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(metricService.evaluateManualRange(principal.tenantId(), date, endDate));
    }
}
