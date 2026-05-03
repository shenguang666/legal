package com.legal.knowledge.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.legal.common.ApiResponse;
import com.legal.knowledge.dto.KnowledgeQaIndexConfigDto;
import com.legal.knowledge.dto.KnowledgeQaIndexConfigUpdateRequest;
import com.legal.knowledge.service.KnowledgeQaIndexConfigService;
import com.legal.knowledge.service.KnowledgeService;
import com.legal.security.AuthContextHolder;
import com.legal.security.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@SaCheckRole("ADMIN")
@RestController
@RequestMapping("/api/knowledge/admin")
public class KnowledgeAdminController {

    private final KnowledgeService knowledgeService;
    private final KnowledgeQaIndexConfigService qaIndexConfigService;

    public KnowledgeAdminController(KnowledgeService knowledgeService,
                                    KnowledgeQaIndexConfigService qaIndexConfigService) {
        this.knowledgeService = knowledgeService;
        this.qaIndexConfigService = qaIndexConfigService;
    }

    @PostMapping("/reset")
    public ApiResponse<Map<String, Object>> reset(@RequestParam(value = "purgeDb", defaultValue = "false") boolean purgeDb) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(knowledgeService.resetForEvaluation(principal, purgeDb));
    }

    @GetMapping("/qa-index-config")
    public ApiResponse<KnowledgeQaIndexConfigDto> getQaIndexConfig() {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(qaIndexConfigService.getConfig(principal));
    }

    @PostMapping("/qa-index-config")
    public ApiResponse<KnowledgeQaIndexConfigDto> updateQaIndexConfig(@Valid @org.springframework.web.bind.annotation.RequestBody KnowledgeQaIndexConfigUpdateRequest request) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(qaIndexConfigService.updateConfig(principal, request.getIndexScope()));
    }
}
