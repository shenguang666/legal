package com.legal.knowledge.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.legal.common.ApiResponse;
import com.legal.knowledge.dto.DocumentProcessingCapabilitiesDto;
import com.legal.knowledge.service.DocumentProcessingCapabilityService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SaCheckRole("ADMIN")
@Validated
@RestController
@RequestMapping("/api/document-processing")
public class DocumentProcessingController {

    private final DocumentProcessingCapabilityService capabilityService;

    public DocumentProcessingController(DocumentProcessingCapabilityService capabilityService) {
        this.capabilityService = capabilityService;
    }

    @GetMapping("/capabilities")
    public ApiResponse<DocumentProcessingCapabilitiesDto> capabilities() {
        return ApiResponse.ok(capabilityService.capabilities());
    }
}
