package com.legal.contract.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.legal.common.ApiResponse;
import com.legal.contract.dto.ContractReviewDetailDto;
import com.legal.contract.dto.ContractReviewTriggerRequest;
import com.legal.contract.service.ContractReviewService;
import com.legal.security.AuthContextHolder;
import com.legal.security.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SaCheckRole("ADMIN")
@Validated
@RestController
@RequestMapping("/api/tianyan/reviews")
public class TianyanReviewController {

    private final ContractReviewService contractReviewService;

    public TianyanReviewController(ContractReviewService contractReviewService) {
        this.contractReviewService = contractReviewService;
    }

    @PostMapping
    public ApiResponse<ContractReviewDetailDto> trigger(@Valid @RequestBody ContractReviewTriggerRequest request) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(contractReviewService.triggerReview(principal, request));
    }

    @PostMapping("/{reviewId}/rerun")
    public ApiResponse<ContractReviewDetailDto> rerun(@PathVariable Long reviewId,
                                                      @RequestParam("requestId") String requestId) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(contractReviewService.rerunReview(principal, reviewId, requestId));
    }

    @GetMapping("/{reviewId}")
    public ApiResponse<ContractReviewDetailDto> detail(@PathVariable Long reviewId) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(contractReviewService.getReviewDetail(principal, reviewId));
    }

    @GetMapping("/by-document/{documentId}")
    public ApiResponse<ContractReviewDetailDto> latestByDocument(@PathVariable Long documentId) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(contractReviewService.getLatestReviewByDocument(principal, documentId));
    }
}
