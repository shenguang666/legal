package com.legal.knowledge.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.legal.common.ApiResponse;
import com.legal.knowledge.service.KnowledgeService;
import com.legal.security.AuthContextHolder;
import com.legal.security.AuthPrincipal;
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

    public KnowledgeAdminController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @PostMapping("/reset")
    public ApiResponse<Map<String, Object>> reset(@RequestParam(value = "purgeDb", defaultValue = "false") boolean purgeDb) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(knowledgeService.resetForEvaluation(principal, purgeDb));
    }
}
