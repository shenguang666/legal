package com.legal.feedback.controller;

import com.legal.common.ApiResponse;
import com.legal.feedback.dto.FeedbackRequest;
import com.legal.feedback.service.FeedbackService;
import com.legal.security.AuthContextHolder;
import com.legal.security.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> submit(@Valid @RequestBody FeedbackRequest request) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        Long feedbackId = feedbackService.submit(principal, request);
        return ApiResponse.ok(Map.of("feedbackId", feedbackId));
    }
}
