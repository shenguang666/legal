package com.legal.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.legal.common.AppException;
import com.legal.config.ElasticsearchProperties;
import com.legal.enums.QaKnowledgeIndexScope;
import com.legal.knowledge.dto.KnowledgeQaIndexConfigDto;
import com.legal.knowledge.entity.KnowledgeQaIndexConfigEntity;
import com.legal.knowledge.mapper.KnowledgeQaIndexConfigMapper;
import com.legal.security.AuthPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class KnowledgeQaIndexConfigService {

    private final KnowledgeQaIndexConfigMapper configMapper;
    private final ElasticsearchProperties elasticsearchProperties;

    public KnowledgeQaIndexConfigService(KnowledgeQaIndexConfigMapper configMapper,
                                         ElasticsearchProperties elasticsearchProperties) {
        this.configMapper = configMapper;
        this.elasticsearchProperties = elasticsearchProperties;
    }

    public KnowledgeQaIndexConfigDto getConfig(AuthPrincipal principal) {
        QaKnowledgeIndexScope scope = resolveScope(principal.tenantId());
        return toDto(scope);
    }

    public QaKnowledgeIndexScope resolveScope(Long tenantId) {
        KnowledgeQaIndexConfigEntity entity = configMapper.selectOne(new LambdaQueryWrapper<KnowledgeQaIndexConfigEntity>()
                .eq(KnowledgeQaIndexConfigEntity::getTenantId, tenantId)
                .last("limit 1"));
        if (entity == null || entity.getIndexScope() == null) {
            return defaultScope();
        }
        return entity.getIndexScope();
    }

    @Transactional
    public KnowledgeQaIndexConfigDto updateConfig(AuthPrincipal principal, String indexScope) {
        QaKnowledgeIndexScope scope = parseScope(indexScope);
        LocalDateTime now = LocalDateTime.now();
        KnowledgeQaIndexConfigEntity entity = configMapper.selectOne(new LambdaQueryWrapper<KnowledgeQaIndexConfigEntity>()
                .eq(KnowledgeQaIndexConfigEntity::getTenantId, principal.tenantId())
                .last("limit 1"));
        if (entity == null) {
            entity = new KnowledgeQaIndexConfigEntity();
            entity.setTenantId(principal.tenantId());
            entity.setIndexScope(scope);
            entity.setCreatedBy(principal.userId());
            entity.setUpdatedBy(principal.userId());
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            configMapper.insert(entity);
        } else {
            entity.setIndexScope(scope);
            entity.setUpdatedBy(principal.userId());
            entity.setUpdatedAt(now);
            configMapper.updateById(entity);
        }
        return toDto(scope);
    }

    public List<String> resolveIndexNames(QaKnowledgeIndexScope scope) {
        QaKnowledgeIndexScope safeScope = scope == null ? defaultScope() : scope;
        String nativeIndex = elasticsearchProperties.getIndex().getKbChunks();
        String mineruIndex = elasticsearchProperties.getIndex().getKbChunksMineru();
        return switch (safeScope) {
            case NATIVE_ONLY -> List.of(nativeIndex);
            case MINERU_ONLY -> List.of(mineruIndex);
            case BOTH -> List.of(nativeIndex, mineruIndex);
        };
    }

    private KnowledgeQaIndexConfigDto toDto(QaKnowledgeIndexScope scope) {
        KnowledgeQaIndexConfigDto dto = new KnowledgeQaIndexConfigDto();
        dto.setIndexScope(scope.getCode());
        dto.setAvailableScopes(Arrays.stream(QaKnowledgeIndexScope.values()).map(QaKnowledgeIndexScope::getCode).toList());
        dto.setNativeIndexName(elasticsearchProperties.getIndex().getKbChunks());
        dto.setMineruIndexName(elasticsearchProperties.getIndex().getKbChunksMineru());
        return dto;
    }

    private QaKnowledgeIndexScope defaultScope() {
        QaKnowledgeIndexScope configured = elasticsearchProperties.getSearch().getDefaultKnowledgeIndexScope();
        return configured == null ? QaKnowledgeIndexScope.NATIVE_ONLY : configured;
    }

    private QaKnowledgeIndexScope parseScope(String value) {
        if (!StringUtils.hasText(value)) {
            throw AppException.badRequest("智能问答知识库检索索引范围不能为空");
        }
        for (QaKnowledgeIndexScope scope : QaKnowledgeIndexScope.values()) {
            if (scope.getCode().equalsIgnoreCase(value.trim())) {
                return scope;
            }
        }
        throw AppException.badRequest("不支持的智能问答知识库检索索引范围: " + value);
    }
}
