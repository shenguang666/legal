package com.legal.knowledge.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.legal.common.ApiResponse;
import com.legal.knowledge.dto.DocumentDto;
import com.legal.knowledge.service.RiskRuleDocumentService;
import com.legal.security.AuthContextHolder;
import com.legal.security.AuthPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@SaCheckRole("ADMIN")
@Validated
@RestController
@RequestMapping("/api/risk-rules/documents")
public class RiskRuleDocumentController {

    private final RiskRuleDocumentService riskRuleDocumentService;

    public RiskRuleDocumentController(RiskRuleDocumentService riskRuleDocumentService) {
        this.riskRuleDocumentService = riskRuleDocumentService;
    }

    @PostMapping("/import")
    public ApiResponse<DocumentDto> importFile(@RequestParam("requestId") String requestId,
                                               @RequestParam("file") MultipartFile file,
                                               @RequestParam(value = "title", required = false) String title,
                                               @RequestParam(value = "source", required = false) String source,
                                               @RequestParam(value = "chunkSize", required = false) Integer chunkSize,
                                               @RequestParam(value = "chunkOverlap", required = false) Integer chunkOverlap,
                                               @RequestParam(value = "parseMethod", required = false) String parseMethod,
                                               @RequestParam(value = "cleaningEnabled", required = false) Boolean cleaningEnabled) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(riskRuleDocumentService.importDocument(principal, requestId, file, title, source, chunkSize, chunkOverlap, parseMethod, cleaningEnabled));
    }

    @GetMapping
    public ApiResponse<List<DocumentDto>> list() {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(riskRuleDocumentService.listDocuments(principal));
    }

    @PostMapping("/{id}/index")
    public ApiResponse<DocumentDto> index(@PathVariable("id") Long id,
                                          @RequestParam("requestId") String requestId) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(riskRuleDocumentService.triggerIndex(principal, id, requestId));
    }

    @PostMapping("/{id}/retry-parse")
    public ApiResponse<DocumentDto> retryParse(@PathVariable("id") Long id,
                                               @RequestParam("requestId") String requestId) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(riskRuleDocumentService.retryParsing(principal, id, requestId));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<DocumentDto> delete(@PathVariable("id") Long id,
                                           @RequestParam("requestId") String requestId) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(riskRuleDocumentService.deleteDocument(principal, id, requestId));
    }
}
