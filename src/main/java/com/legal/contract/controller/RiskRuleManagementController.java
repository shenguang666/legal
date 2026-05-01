package com.legal.contract.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.legal.common.ApiResponse;
import com.legal.contract.dto.ContractFieldDefinitionDto;
import com.legal.contract.dto.ContractFieldDefinitionStatusUpdateRequest;
import com.legal.contract.dto.ContractFieldDefinitionUpsertRequest;
import com.legal.contract.dto.RiskRuleManageDto;
import com.legal.contract.dto.RiskRuleManualCreateRequest;
import com.legal.contract.dto.RiskRuleStatusUpdateRequest;
import com.legal.contract.service.ContractFieldDefinitionService;
import com.legal.contract.service.RiskRuleManagementService;
import com.legal.security.AuthContextHolder;
import com.legal.security.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@SaCheckRole("ADMIN")
@Validated
@RestController
@RequestMapping("/api/risk-rules")
public class RiskRuleManagementController {

    private final RiskRuleManagementService riskRuleManagementService;
    private final ContractFieldDefinitionService contractFieldDefinitionService;

    public RiskRuleManagementController(RiskRuleManagementService riskRuleManagementService,
                                        ContractFieldDefinitionService contractFieldDefinitionService) {
        this.riskRuleManagementService = riskRuleManagementService;
        this.contractFieldDefinitionService = contractFieldDefinitionService;
    }

    @GetMapping("/entries")
    public ApiResponse<List<RiskRuleManageDto>> listRules() {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(riskRuleManagementService.listRules(principal));
    }

    @PostMapping("/manual")
    public ApiResponse<RiskRuleManageDto> createManualRule(@Valid @RequestBody RiskRuleManualCreateRequest request) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(riskRuleManagementService.createManualRule(principal, request));
    }

    @PostMapping("/import")
    public ApiResponse<RiskRuleManageDto> importRuleFile(@RequestParam("requestId") String requestId,
                                                         @RequestParam("file") MultipartFile file,
                                                         @RequestParam(value = "title", required = false) String title,
                                                         @RequestParam(value = "source", required = false) String source,
                                                         @RequestParam(value = "ruleCode", required = false) String ruleCode,
                                                         @RequestParam(value = "ruleName", required = false) String ruleName,
                                                         @RequestParam(value = "severity", required = false) String severity,
                                                         @RequestParam(value = "hitThreshold", required = false) Double hitThreshold,
                                                         @RequestParam(value = "chunkSize", required = false) Integer chunkSize,
                                                         @RequestParam(value = "chunkOverlap", required = false) Integer chunkOverlap) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(riskRuleManagementService.importRuleFile(
                principal,
                requestId,
                file,
                title,
                source,
                ruleCode,
                ruleName,
                severity,
                hitThreshold,
                chunkSize,
                chunkOverlap
        ));
    }

    @PostMapping("/{ruleId}/status")
    public ApiResponse<RiskRuleManageDto> updateRuleStatus(@PathVariable Long ruleId,
                                                           @Valid @RequestBody RiskRuleStatusUpdateRequest request) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(riskRuleManagementService.updateRuleStatus(principal, ruleId, request));
    }

    @PostMapping("/{ruleId}/reindex")
    public ApiResponse<RiskRuleManageDto> reindex(@PathVariable Long ruleId,
                                                  @RequestParam("requestId") String requestId) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(riskRuleManagementService.reindexRule(principal, ruleId, requestId));
    }

    @DeleteMapping("/{ruleId}")
    public ApiResponse<Void> deleteRule(@PathVariable Long ruleId,
                                        @RequestParam("requestId") String requestId) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        riskRuleManagementService.deleteRule(principal, ruleId, requestId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/fields")
    public ApiResponse<List<ContractFieldDefinitionDto>> listFieldDefinitions() {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(contractFieldDefinitionService.listDefinitions(principal));
    }

    @PostMapping("/fields")
    public ApiResponse<ContractFieldDefinitionDto> createFieldDefinition(@Valid @RequestBody ContractFieldDefinitionUpsertRequest request) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(contractFieldDefinitionService.createDefinition(principal, request));
    }

    @PostMapping("/fields/{fieldDefinitionId}")
    public ApiResponse<ContractFieldDefinitionDto> updateFieldDefinition(@PathVariable Long fieldDefinitionId,
                                                                         @Valid @RequestBody ContractFieldDefinitionUpsertRequest request) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(contractFieldDefinitionService.updateDefinition(principal, fieldDefinitionId, request));
    }

    @PostMapping("/fields/{fieldDefinitionId}/status")
    public ApiResponse<ContractFieldDefinitionDto> updateFieldDefinitionStatus(@PathVariable Long fieldDefinitionId,
                                                                               @Valid @RequestBody ContractFieldDefinitionStatusUpdateRequest request) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(contractFieldDefinitionService.updateStatus(principal, fieldDefinitionId, request));
    }

    @DeleteMapping("/fields/{fieldDefinitionId}")
    public ApiResponse<Void> deleteFieldDefinition(@PathVariable Long fieldDefinitionId,
                                                   @RequestParam("requestId") String requestId) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        contractFieldDefinitionService.deleteDefinition(principal, fieldDefinitionId, requestId);
        return ApiResponse.ok(null);
    }
}
