package com.legal.retrieval.metrics.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.legal.common.ApiResponse;
import com.legal.retrieval.metrics.dto.RagRetrievalMetricDtos;
import com.legal.retrieval.metrics.service.RagRetrievalMetricService;
import com.legal.security.AuthContextHolder;
import com.legal.security.AuthPrincipal;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@SaCheckRole("ADMIN")
@Validated
@RestController
@RequestMapping("/api/rag-metrics")
public class RagRetrievalMetricController {

    private final RagRetrievalMetricService metricService;

    public RagRetrievalMetricController(RagRetrievalMetricService metricService) {
        this.metricService = metricService;
    }

    @GetMapping("/summary")
    public ApiResponse<RagRetrievalMetricDtos.Summary> summary(@RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                               @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(metricService.summary(principal.tenantId(), startDate, endDate));
    }

    @GetMapping("/details")
    public ApiResponse<RagRetrievalMetricDtos.DetailPage> details(@RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                                  @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                                  @RequestParam(value = "dailySummaryId", required = false) Long dailySummaryId,
                                                                  @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
                                                                  @RequestParam(value = "pageSize", defaultValue = "20") int pageSize) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(metricService.details(principal.tenantId(), startDate, endDate, dailySummaryId, pageNo, pageSize));
    }

    @GetMapping("/daily-summaries")
    public ApiResponse<RagRetrievalMetricDtos.DailySummaryPage> dailySummaries(@RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                                               @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                                               @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
                                                                               @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(metricService.dailySummaries(principal.tenantId(), startDate, endDate, pageNo, pageSize));
    }

    @PostMapping("/run")
    public ApiResponse<RagRetrievalMetricDtos.RunResult> run(@RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(metricService.evaluateDate(date));
    }
}
