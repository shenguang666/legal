package com.legal.court.controller;

import com.legal.common.ApiResponse;
import com.legal.court.entity.CourtSupplementSuggestionEntity;
import com.legal.court.service.CourtSuggestionService;
import com.legal.enums.CourtSuggestionStatus;
import com.legal.security.AuthContextHolder;
import com.legal.security.AuthPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 智能小法庭补证建议 Controller。
 */
@Validated
@RestController
@RequestMapping("/api/smart-court/cases")
public class SmartCourtSuggestionController {

    private final CourtSuggestionService suggestionService;

    public SmartCourtSuggestionController(CourtSuggestionService suggestionService) {
        this.suggestionService = suggestionService;
    }

    @GetMapping("/{caseId}/suggestions")
    public ApiResponse<List<CourtSupplementSuggestionEntity>> list(@PathVariable Long caseId,
                                                                   @RequestParam(value = "status", required = false) CourtSuggestionStatus status) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(suggestionService.listSuggestions(principal, caseId, status));
    }

    @PostMapping("/{caseId}/suggestions/{suggestionId}/ignore")
    public ApiResponse<Void> ignore(@PathVariable Long caseId, @PathVariable Long suggestionId) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        suggestionService.ignoreSuggestion(principal, caseId, suggestionId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{caseId}/suggestions/{suggestionId}/resolve")
    public ApiResponse<Void> resolve(@PathVariable Long caseId, @PathVariable Long suggestionId) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        suggestionService.resolveSuggestion(principal, caseId, suggestionId);
        return ApiResponse.ok(null);
    }
}
