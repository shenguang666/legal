package com.legal.contract.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.legal.common.AppException;
import com.legal.contract.dto.ContractFieldDefinitionDto;
import com.legal.contract.dto.ContractFieldDefinitionStatusUpdateRequest;
import com.legal.contract.dto.ContractFieldDefinitionUpsertRequest;
import com.legal.contract.entity.ContractFieldDefinitionEntity;
import com.legal.contract.mapper.ContractFieldDefinitionMapper;
import com.legal.security.AuthPrincipal;
import com.legal.security.IdempotencyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ContractFieldDefinitionService {

    private final ContractFieldDefinitionMapper fieldDefinitionMapper;
    private final IdempotencyService idempotencyService;

    public ContractFieldDefinitionService(ContractFieldDefinitionMapper fieldDefinitionMapper,
                                          IdempotencyService idempotencyService) {
        this.fieldDefinitionMapper = fieldDefinitionMapper;
        this.idempotencyService = idempotencyService;
    }

    public List<ContractFieldDefinitionDto> listDefinitions(AuthPrincipal principal) {
        return resolveEffectiveDefinitions(principal.tenantId()).stream()
                .map(this::toDto)
                .toList();
    }

    public List<ContractFieldDefinitionEntity> listEnabledDefinitions(Long tenantId) {
        return resolveEffectiveDefinitions(tenantId).stream()
                .filter(item -> Boolean.TRUE.equals(item.getEnabled()))
                .toList();
    }

    @Transactional
    public ContractFieldDefinitionDto createDefinition(AuthPrincipal principal, ContractFieldDefinitionUpsertRequest request) {
        idempotencyService.ensureUnique(principal, "risk-rule:create-field-definition", request.getRequestId());
        ensureFieldCodeAvailable(principal.tenantId(), normalizeFieldCode(request.getFieldCode()), null);
        LocalDateTime now = LocalDateTime.now();
        ContractFieldDefinitionEntity entity = new ContractFieldDefinitionEntity();
        entity.setTenantId(principal.tenantId());
        applyEditableFields(entity, request);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        fieldDefinitionMapper.insert(entity);
        return toDto(entity);
    }

    @Transactional
    public ContractFieldDefinitionDto updateDefinition(AuthPrincipal principal,
                                                       Long fieldDefinitionId,
                                                       ContractFieldDefinitionUpsertRequest request) {
        idempotencyService.ensureUnique(principal, "risk-rule:update-field-definition", request.getRequestId());
        ContractFieldDefinitionEntity entity = requireTenantDefinition(principal, fieldDefinitionId);
        ensureFieldCodeAvailable(principal.tenantId(), normalizeFieldCode(request.getFieldCode()), fieldDefinitionId);
        applyEditableFields(entity, request);
        entity.setUpdatedAt(LocalDateTime.now());
        fieldDefinitionMapper.updateById(entity);
        return toDto(entity);
    }

    @Transactional
    public ContractFieldDefinitionDto updateStatus(AuthPrincipal principal,
                                                   Long fieldDefinitionId,
                                                   ContractFieldDefinitionStatusUpdateRequest request) {
        idempotencyService.ensureUnique(principal, "risk-rule:update-field-definition-status", request.getRequestId());
        ContractFieldDefinitionEntity entity = requireTenantDefinition(principal, fieldDefinitionId);
        entity.setEnabled(Boolean.TRUE.equals(request.getEnabled()));
        entity.setUpdatedAt(LocalDateTime.now());
        fieldDefinitionMapper.updateById(entity);
        return toDto(entity);
    }

    @Transactional
    public void deleteDefinition(AuthPrincipal principal, Long fieldDefinitionId, String requestId) {
        idempotencyService.ensureUnique(principal, "risk-rule:delete-field-definition", requestId);
        ContractFieldDefinitionEntity entity = requireTenantDefinition(principal, fieldDefinitionId);
        fieldDefinitionMapper.deleteById(entity.getFieldDefinitionId());
    }

    private List<ContractFieldDefinitionEntity> resolveEffectiveDefinitions(Long tenantId) {
        Map<String, ContractFieldDefinitionEntity> effective = new LinkedHashMap<>();
        for (ContractFieldDefinitionEntity entity : fieldDefinitionMapper.selectDefinitionsForTenant(tenantId)) {
            effective.putIfAbsent(entity.getFieldCode(), entity);
        }
        return effective.values().stream().toList();
    }

    private void applyEditableFields(ContractFieldDefinitionEntity entity, ContractFieldDefinitionUpsertRequest request) {
        entity.setFieldCode(normalizeFieldCode(request.getFieldCode()));
        entity.setFieldName(request.getFieldName().trim());
        entity.setExtractorKind(normalizeExtractorKind(request.getExtractorKind()));
        entity.setPatternExpr(StringUtils.hasText(request.getPatternExpr()) ? request.getPatternExpr().trim() : null);
        entity.setKeywordConfig(StringUtils.hasText(request.getKeywordConfig()) ? request.getKeywordConfig().trim() : null);
        entity.setRepeatable(Boolean.TRUE.equals(request.getRepeatable()));
        entity.setDeduplicateByNormalized(request.getDeduplicateByNormalized() == null || Boolean.TRUE.equals(request.getDeduplicateByNormalized()));
        entity.setEnabled(request.getEnabled() == null || Boolean.TRUE.equals(request.getEnabled()));
        entity.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        entity.setDescription(StringUtils.hasText(request.getDescription()) ? request.getDescription().trim() : null);
    }

    private ContractFieldDefinitionEntity requireTenantDefinition(AuthPrincipal principal, Long fieldDefinitionId) {
        ContractFieldDefinitionEntity entity = fieldDefinitionMapper.selectOne(
                new LambdaQueryWrapper<ContractFieldDefinitionEntity>()
                        .eq(ContractFieldDefinitionEntity::getFieldDefinitionId, fieldDefinitionId)
                        .eq(ContractFieldDefinitionEntity::getTenantId, principal.tenantId())
                        .last("limit 1")
        );
        if (entity == null) {
            throw AppException.notFound("抽取字段定义不存在或不可修改");
        }
        return entity;
    }

    private void ensureFieldCodeAvailable(Long tenantId, String fieldCode, Long excludeId) {
        ContractFieldDefinitionEntity existing = fieldDefinitionMapper.selectOne(
                new LambdaQueryWrapper<ContractFieldDefinitionEntity>()
                        .eq(ContractFieldDefinitionEntity::getTenantId, tenantId)
                        .eq(ContractFieldDefinitionEntity::getFieldCode, fieldCode)
                        .last("limit 1")
        );
        if (existing != null && (excludeId == null || !excludeId.equals(existing.getFieldDefinitionId()))) {
            throw AppException.badRequest("字段编码已存在: " + fieldCode);
        }
    }

    private String normalizeFieldCode(String value) {
        if (!StringUtils.hasText(value)) {
            throw AppException.badRequest("字段编码不能为空");
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeExtractorKind(String value) {
        if (!StringUtils.hasText(value)) {
            throw AppException.badRequest("抽取器类型不能为空");
        }
        String kind = value.trim().toUpperCase(Locale.ROOT);
        return switch (kind) {
            case "PARTY_PATTERN", "AMOUNT_PATTERN", "DATE_KEYWORD", "KEYWORD_LINE" -> kind;
            default -> throw AppException.badRequest("不支持的抽取器类型: " + value);
        };
    }

    private ContractFieldDefinitionDto toDto(ContractFieldDefinitionEntity entity) {
        ContractFieldDefinitionDto dto = new ContractFieldDefinitionDto();
        dto.setFieldDefinitionId(entity.getFieldDefinitionId());
        dto.setTenantId(entity.getTenantId());
        dto.setSystemDefault(entity.getTenantId() != null && entity.getTenantId() == 0L);
        dto.setFieldCode(entity.getFieldCode());
        dto.setFieldName(entity.getFieldName());
        dto.setExtractorKind(entity.getExtractorKind());
        dto.setPatternExpr(entity.getPatternExpr());
        dto.setKeywordConfig(entity.getKeywordConfig());
        dto.setRepeatable(entity.getRepeatable());
        dto.setDeduplicateByNormalized(entity.getDeduplicateByNormalized());
        dto.setEnabled(entity.getEnabled());
        dto.setSortOrder(entity.getSortOrder());
        dto.setDescription(entity.getDescription());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
