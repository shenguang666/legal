package com.legal.contract.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.legal.common.AppException;
import com.legal.contract.dto.RiskRuleManageDto;
import com.legal.contract.dto.RiskRuleManualCreateRequest;
import com.legal.contract.dto.RiskRuleStatusUpdateRequest;
import com.legal.contract.entity.ContractRuleDefinitionEntity;
import com.legal.contract.mapper.ContractRuleDefinitionMapper;
import com.legal.enums.ContractRiskSeverity;
import com.legal.enums.ContractRuleType;
import com.legal.knowledge.dto.DocumentDto;
import com.legal.knowledge.entity.KbDocumentEntity;
import com.legal.knowledge.mapper.KbDocumentMapper;
import com.legal.knowledge.service.RiskRuleDocumentService;
import com.legal.security.AuthPrincipal;
import com.legal.security.IdempotencyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RiskRuleManagementService {

    private final ContractRuleDefinitionMapper ruleDefinitionMapper;
    private final KbDocumentMapper kbDocumentMapper;
    private final RiskRuleDocumentService riskRuleDocumentService;
    private final IdempotencyService idempotencyService;

    public RiskRuleManagementService(ContractRuleDefinitionMapper ruleDefinitionMapper,
                                     KbDocumentMapper kbDocumentMapper,
                                     RiskRuleDocumentService riskRuleDocumentService,
                                     IdempotencyService idempotencyService) {
        this.ruleDefinitionMapper = ruleDefinitionMapper;
        this.kbDocumentMapper = kbDocumentMapper;
        this.riskRuleDocumentService = riskRuleDocumentService;
        this.idempotencyService = idempotencyService;
    }

    public List<RiskRuleManageDto> listRules(AuthPrincipal principal) {
        List<ContractRuleDefinitionEntity> rules = ruleDefinitionMapper.selectList(
                new LambdaQueryWrapper<ContractRuleDefinitionEntity>()
                        .eq(ContractRuleDefinitionEntity::getTenantId, principal.tenantId())
                        .eq(ContractRuleDefinitionEntity::getRuleType, ContractRuleType.DOCUMENT_RETRIEVAL)
                        .orderByDesc(ContractRuleDefinitionEntity::getUpdatedAt)
                        .orderByDesc(ContractRuleDefinitionEntity::getRuleId)
        );
        Map<Long, KbDocumentEntity> documentMap = loadDocumentMap(rules.stream()
                .map(ContractRuleDefinitionEntity::getDocumentId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet()));
        return rules.stream().map(rule -> toDto(rule, documentMap.get(rule.getDocumentId()))).toList();
    }

    @Transactional
    public RiskRuleManageDto createManualRule(AuthPrincipal principal, RiskRuleManualCreateRequest request) {
        DocumentDto document = riskRuleDocumentService.createManualDocument(
                principal,
                request.getRequestId(),
                request.getRuleContent(),
                request.getRuleName(),
                StringUtils.hasText(request.getSource()) ? request.getSource().trim() : "手工录入风险规则",
                null,
                null
        );
        ContractRuleDefinitionEntity entity = createDocumentRetrievalRule(
                principal,
                normalizeRuleCode(principal.tenantId(), request.getRuleCode()),
                request.getRuleName().trim(),
                "MANUAL_TEXT",
                document.getDocumentId(),
                parseSeverity(request.getSeverity()),
                normalizeThreshold(request.getHitThreshold()),
                request.getRuleContent().trim()
        );
        KbDocumentEntity linkedDocument = kbDocumentMapper.selectById(document.getDocumentId());
        return toDto(entity, linkedDocument);
    }

    @Transactional
    public RiskRuleManageDto importRuleFile(AuthPrincipal principal,
                                            String requestId,
                                            MultipartFile file,
                                            String title,
                                            String source,
                                            String ruleCode,
                                            String ruleName,
                                            String severity,
                                            Double hitThreshold,
                                            Integer chunkSize,
                                            Integer chunkOverlap,
                                            String parseMethod,
                                            Boolean cleaningEnabled) {
        DocumentDto document = riskRuleDocumentService.importDocument(principal, requestId, file, title, source, chunkSize, chunkOverlap, parseMethod, cleaningEnabled);
        String resolvedRuleName = StringUtils.hasText(ruleName) ? ruleName.trim() : document.getTitle();
        ContractRuleDefinitionEntity entity = createDocumentRetrievalRule(
                principal,
                normalizeRuleCode(principal.tenantId(), ruleCode),
                resolvedRuleName,
                "IMPORTED_DOCUMENT",
                document.getDocumentId(),
                parseSeverity(severity),
                normalizeThreshold(hitThreshold),
                null
        );
        KbDocumentEntity linkedDocument = kbDocumentMapper.selectById(document.getDocumentId());
        return toDto(entity, linkedDocument);
    }

    @Transactional
    public RiskRuleManageDto updateRuleStatus(AuthPrincipal principal, Long ruleId, RiskRuleStatusUpdateRequest request) {
        idempotencyService.ensureUnique(principal, "risk-rule:update-status", request.getRequestId());
        ContractRuleDefinitionEntity rule = requireTenantRule(principal, ruleId);
        rule.setEnabled(Boolean.TRUE.equals(request.getEnabled()));
        rule.setUpdatedAt(LocalDateTime.now());
        ruleDefinitionMapper.updateById(rule);
        return toDto(rule, loadDocumentMap(Set.of(rule.getDocumentId())).get(rule.getDocumentId()));
    }

    @Transactional
    public RiskRuleManageDto reindexRule(AuthPrincipal principal, Long ruleId, String requestId) {
        ContractRuleDefinitionEntity rule = requireTenantRule(principal, ruleId);
        if (rule.getDocumentId() == null) {
            throw AppException.badRequest("当前风险规则未关联文档，无法重建索引");
        }
        riskRuleDocumentService.triggerIndex(principal, rule.getDocumentId(), requestId);
        KbDocumentEntity linkedDocument = kbDocumentMapper.selectById(rule.getDocumentId());
        return toDto(rule, linkedDocument);
    }

    @Transactional
    public void deleteRule(AuthPrincipal principal, Long ruleId, String requestId) {
        idempotencyService.ensureUnique(principal, "risk-rule:delete-rule", requestId);
        ContractRuleDefinitionEntity rule = requireTenantRule(principal, ruleId);
        if (rule.getDocumentId() != null) {
            riskRuleDocumentService.deleteDocument(principal, rule.getDocumentId(), requestId);
        }
        ruleDefinitionMapper.deleteById(rule.getRuleId());
    }

    private ContractRuleDefinitionEntity createDocumentRetrievalRule(AuthPrincipal principal,
                                                                     String ruleCode,
                                                                     String ruleName,
                                                                     String ruleSourceType,
                                                                     Long documentId,
                                                                     ContractRiskSeverity severity,
                                                                     BigDecimal hitThreshold,
                                                                     String ruleContent) {
        ensureRuleCodeAvailable(principal.tenantId(), ruleCode, null);
        LocalDateTime now = LocalDateTime.now();
        ContractRuleDefinitionEntity entity = new ContractRuleDefinitionEntity();
        entity.setTenantId(principal.tenantId());
        entity.setRuleCode(ruleCode);
        entity.setRuleName(ruleName);
        entity.setRuleType(ContractRuleType.DOCUMENT_RETRIEVAL);
        entity.setRuleSourceType(ruleSourceType);
        entity.setFieldCode(null);
        entity.setDocumentId(documentId);
        entity.setSeverity(severity);
        entity.setEnabled(true);
        entity.setHitThreshold(hitThreshold);
        entity.setRuleContent(ruleContent);
        entity.setRuleParams(null);
        entity.setSortOrder(resolveNextSortOrder(principal.tenantId()));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        ruleDefinitionMapper.insert(entity);
        return entity;
    }

    private Map<Long, KbDocumentEntity> loadDocumentMap(Set<Long> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, KbDocumentEntity> documentMap = new LinkedHashMap<>();
        for (KbDocumentEntity entity : kbDocumentMapper.selectBatchIds(documentIds)) {
            documentMap.put(entity.getDocumentId(), entity);
        }
        return documentMap;
    }

    private ContractRuleDefinitionEntity requireTenantRule(AuthPrincipal principal, Long ruleId) {
        ContractRuleDefinitionEntity rule = ruleDefinitionMapper.selectOne(
                new LambdaQueryWrapper<ContractRuleDefinitionEntity>()
                        .eq(ContractRuleDefinitionEntity::getRuleId, ruleId)
                        .eq(ContractRuleDefinitionEntity::getTenantId, principal.tenantId())
                        .eq(ContractRuleDefinitionEntity::getRuleType, ContractRuleType.DOCUMENT_RETRIEVAL)
                        .last("limit 1")
        );
        if (rule == null) {
            throw AppException.notFound("风险规则不存在");
        }
        return rule;
    }

    private void ensureRuleCodeAvailable(Long tenantId, String ruleCode, Long excludeRuleId) {
        ContractRuleDefinitionEntity existing = ruleDefinitionMapper.selectOne(
                new LambdaQueryWrapper<ContractRuleDefinitionEntity>()
                        .eq(ContractRuleDefinitionEntity::getTenantId, tenantId)
                        .eq(ContractRuleDefinitionEntity::getRuleCode, ruleCode)
                        .last("limit 1")
        );
        if (existing != null && (excludeRuleId == null || !excludeRuleId.equals(existing.getRuleId()))) {
            throw AppException.badRequest("风险规则编码已存在: " + ruleCode);
        }
    }

    private int resolveNextSortOrder(Long tenantId) {
        Long count = ruleDefinitionMapper.selectCount(
                new LambdaQueryWrapper<ContractRuleDefinitionEntity>()
                        .eq(ContractRuleDefinitionEntity::getTenantId, tenantId)
                        .eq(ContractRuleDefinitionEntity::getRuleType, ContractRuleType.DOCUMENT_RETRIEVAL)
        );
        long safe = count == null ? 0L : count;
        return (int) ((safe + 1L) * 10L);
    }

    private String normalizeRuleCode(Long tenantId, String requestedRuleCode) {
        if (StringUtils.hasText(requestedRuleCode)) {
            return requestedRuleCode.trim().toUpperCase(Locale.ROOT);
        }
        return ("RISK_RULE_" + tenantId + "_" + System.currentTimeMillis()).toUpperCase(Locale.ROOT);
    }

    private ContractRiskSeverity parseSeverity(String value) {
        if (!StringUtils.hasText(value)) {
            return ContractRiskSeverity.MEDIUM;
        }
        try {
            return ContractRiskSeverity.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw AppException.badRequest("不支持的风险级别: " + value);
        }
    }

    private BigDecimal normalizeThreshold(Double value) {
        double resolved = value == null ? 0.78d : value;
        if (resolved < 0D || resolved > 1D) {
            throw AppException.badRequest("命中阈值必须位于 0 到 1 之间");
        }
        return BigDecimal.valueOf(resolved);
    }

    private RiskRuleManageDto toDto(ContractRuleDefinitionEntity rule, KbDocumentEntity document) {
        RiskRuleManageDto dto = new RiskRuleManageDto();
        dto.setRuleId(rule.getRuleId());
        dto.setRuleCode(rule.getRuleCode());
        dto.setRuleName(rule.getRuleName());
        dto.setRuleType(rule.getRuleType() == null ? null : rule.getRuleType().getCode());
        dto.setRuleSourceType(rule.getRuleSourceType());
        dto.setSeverity(rule.getSeverity() == null ? null : rule.getSeverity().getCode());
        dto.setEnabled(rule.getEnabled());
        dto.setHitThreshold(rule.getHitThreshold() == null ? null : rule.getHitThreshold().doubleValue());
        dto.setDocumentId(rule.getDocumentId());
        dto.setRuleContent(rule.getRuleContent());
        dto.setCreatedAt(rule.getCreatedAt());
        dto.setUpdatedAt(rule.getUpdatedAt());
        if (document != null) {
            dto.setDocumentOwnerUserId(document.getOwnerUserId());
            dto.setDocumentOwnerUsername(document.getOwnerUsername());
            dto.setDocumentTitle(document.getTitle());
            dto.setDocumentSource(document.getSource());
            dto.setDocumentUrl(document.getDocumentUrl());
            dto.setDocumentStatus(document.getStatus() == null ? null : document.getStatus().getCode());
            dto.setDocumentIndexStatus(document.getIndexStatus() == null ? null : document.getIndexStatus().getCode());
            dto.setDocumentParseMethod(document.getParseMethod() == null ? null : document.getParseMethod().getCode());
            dto.setDocumentParseStatus(document.getParseStatus() == null ? null : document.getParseStatus().getCode());
            dto.setDocumentParseFailureReason(document.getParseFailureReason());
            dto.setDocumentCreatedAt(document.getCreatedAt());
        }
        return dto;
    }
}
