package com.legal.court.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.legal.common.AppException;
import com.legal.court.dto.CourtCaseCreateRequest;
import com.legal.court.dto.CourtCaseUpdateRequest;
import com.legal.court.entity.CourtCaseEntity;
import com.legal.court.entity.CourtCaseEvidenceEntity;
import com.legal.court.graph.CourtGraphEventPayload;
import com.legal.court.graph.CourtGraphEventPublisher;
import com.legal.court.mapper.CourtCaseEvidenceMapper;
import com.legal.court.mapper.CourtCaseMapper;
import com.legal.enums.CourtCaseStatus;
import com.legal.enums.CourtCaseType;
import com.legal.enums.CourtEvidenceStatus;
import com.legal.enums.CourtGraphEventType;
import com.legal.enums.KbDocumentBizType;
import com.legal.enums.KbDocumentStatus;
import com.legal.knowledge.entity.KbDocumentEntity;
import com.legal.knowledge.mapper.KbDocumentMapper;
import com.legal.security.AuthPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 智能小法庭案件生命周期服务。
 */
@Service
@Slf4j
public class CourtCaseService {

    private static final Set<KbDocumentBizType> ALLOWED_DOCUMENT_BIZ_TYPES = Set.of(KbDocumentBizType.KNOWLEDGE, KbDocumentBizType.RISK_RULE);

    private final CourtCaseMapper courtCaseMapper;
    private final CourtCaseEvidenceMapper courtCaseEvidenceMapper;
    private final KbDocumentMapper kbDocumentMapper;
    private final CourtGraphEventPublisher graphEventPublisher;

    public CourtCaseService(CourtCaseMapper courtCaseMapper,
                            CourtCaseEvidenceMapper courtCaseEvidenceMapper,
                            KbDocumentMapper kbDocumentMapper,
                            CourtGraphEventPublisher graphEventPublisher) {
        this.courtCaseMapper = courtCaseMapper;
        this.courtCaseEvidenceMapper = courtCaseEvidenceMapper;
        this.kbDocumentMapper = kbDocumentMapper;
        this.graphEventPublisher = graphEventPublisher;
    }

    /**
     * 创建智能小法庭案件，并登记用户选择的证据文档。
     */
    @Transactional
    public CourtCaseEntity createCase(AuthPrincipal principal, CourtCaseCreateRequest request) {
        CourtCaseEntity entity = new CourtCaseEntity();
        entity.setTenantId(principal.tenantId());
        entity.setOwnerUserId(principal.userId());
        entity.setTitle(StringUtils.hasText(request.getTitle()) ? request.getTitle() : "合同纠纷模拟案件");
        entity.setCaseType(CourtCaseType.CONTRACT_DISPUTE);
        entity.setUserSide(request.getUserSide());
        entity.setStatus(CourtCaseStatus.DRAFT);
        entity.setCaseSummary(request.getCaseSummary());
        entity.setUserObjective(request.getUserObjective());
        entity.setFactsConfirmed(false);
        entity.setTotalRounds(0);
        entity.setTotalTokens(0L);
        entity.setGraphState("PROJECTING");
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        courtCaseMapper.insert(entity);
        log.info("court.case.create tenantId={} userId={} caseId={} title={}", principal.tenantId(), principal.userId(), entity.getCaseId(), entity.getTitle());
        for (Long documentId : request.getDocumentIds() == null ? List.<Long>of() : request.getDocumentIds()) {
            KbDocumentEntity document = requireAllowedDocument(principal, documentId);
            addEvidence(entity, document);
        }
        publishCaseNode(entity);
        return entity;
    }

    /**
     * 查询案件，跨租户或跨用户访问统一返回不存在。
     */
    public CourtCaseEntity requireCase(AuthPrincipal principal, Long caseId) {
        CourtCaseEntity entity = courtCaseMapper.selectOne(new LambdaQueryWrapper<CourtCaseEntity>()
                .eq(CourtCaseEntity::getTenantId, principal.tenantId())
                .eq(CourtCaseEntity::getOwnerUserId, principal.userId())
                .eq(CourtCaseEntity::getCaseId, caseId)
                .ne(CourtCaseEntity::getStatus, CourtCaseStatus.DELETED)
                .last("limit 1"));
        if (entity == null) {
            throw AppException.notFound("案件不存在");
        }
        return entity;
    }

    /**
     * 分页查询当前用户的智能小法庭案件。
     */
    public Page<CourtCaseEntity> pageCases(AuthPrincipal principal, int pageNo, int pageSize) {
        int safePageNo = Math.max(1, pageNo);
        int safePageSize = Math.max(1, Math.min(pageSize, 100));
        return courtCaseMapper.selectPage(Page.of(safePageNo, safePageSize), new LambdaQueryWrapper<CourtCaseEntity>()
                .eq(CourtCaseEntity::getTenantId, principal.tenantId())
                .eq(CourtCaseEntity::getOwnerUserId, principal.userId())
                .ne(CourtCaseEntity::getStatus, CourtCaseStatus.DELETED)
                .orderByDesc(CourtCaseEntity::getUpdatedAt));
    }

    /**
     * 更新案件基础信息。
     */
    @Transactional
    public CourtCaseEntity updateCase(AuthPrincipal principal, Long caseId, CourtCaseUpdateRequest request) {
        CourtCaseEntity entity = requireCase(principal, caseId);
        if (StringUtils.hasText(request.getTitle())) {
            entity.setTitle(request.getTitle());
        }
        if (request.getUserSide() != null) {
            entity.setUserSide(request.getUserSide());
        }
        if (request.getCaseSummary() != null) {
            entity.setCaseSummary(request.getCaseSummary());
        }
        if (request.getUserObjective() != null) {
            entity.setUserObjective(request.getUserObjective());
        }
        entity.setUpdatedAt(LocalDateTime.now());
        courtCaseMapper.updateById(entity);
        publishCaseNode(entity);
        return entity;
    }

    /**
     * 确认案件要素，案件进入可开庭状态。
     */
    @Transactional
    public CourtCaseEntity confirmFacts(AuthPrincipal principal, Long caseId) {
        CourtCaseEntity entity = requireCase(principal, caseId);
        entity.setFactsConfirmed(true);
        entity.setStatus(CourtCaseStatus.READY);
        entity.setUpdatedAt(LocalDateTime.now());
        courtCaseMapper.updateById(entity);
        publishCaseNode(entity);
        return entity;
    }

    /**
     * 开始庭审，案件进入庭审中状态。
     */
    @Transactional
    public CourtCaseEntity startHearing(AuthPrincipal principal, Long caseId) {
        CourtCaseEntity entity = requireCase(principal, caseId);
        if (!Boolean.TRUE.equals(entity.getFactsConfirmed())) {
            throw AppException.badRequest("请先确认案件要素后再开庭");
        }
        entity.setStatus(CourtCaseStatus.HEARING);
        entity.setUpdatedAt(LocalDateTime.now());
        courtCaseMapper.updateById(entity);
        publishCaseNode(entity);
        return entity;
    }

    /**
     * 归档案件。
     */
    @Transactional
    public void archiveCase(AuthPrincipal principal, Long caseId) {
        CourtCaseEntity entity = requireCase(principal, caseId);
        entity.setStatus(CourtCaseStatus.ARCHIVED);
        entity.setUpdatedAt(LocalDateTime.now());
        courtCaseMapper.updateById(entity);
        publishCaseNode(entity);
    }

    /**
     * 软删除案件，并异步发布 Neo4j 子图清理事件。
     */
    @Transactional
    public void softDeleteCase(AuthPrincipal principal, Long caseId) {
        CourtCaseEntity entity = requireCase(principal, caseId);
        entity.setStatus(CourtCaseStatus.DELETED);
        entity.setUpdatedAt(LocalDateTime.now());
        courtCaseMapper.updateById(entity);
        graphEventPublisher.publish(principal.tenantId(), caseId, null, CourtGraphEventType.DELETE_CASE_GRAPH, new CourtGraphEventPayload());
    }

    /**
     * 文档软删除后标记案件证据与证据节点失效，不级联删除案件。
     */
    @Transactional
    public void invalidateEvidenceByDocument(Long tenantId, Long documentId) {
        List<CourtCaseEvidenceEntity> evidences = courtCaseEvidenceMapper.selectList(new LambdaQueryWrapper<CourtCaseEvidenceEntity>()
                .eq(CourtCaseEvidenceEntity::getTenantId, tenantId)
                .eq(CourtCaseEvidenceEntity::getDocumentId, documentId)
                .eq(CourtCaseEvidenceEntity::getStatus, CourtEvidenceStatus.ACTIVE));
        courtCaseEvidenceMapper.invalidateByDocument(tenantId, documentId);
        for (CourtCaseEvidenceEntity evidence : evidences) {
            CourtGraphEventPayload payload = new CourtGraphEventPayload();
            payload.setLabel("Evidence");
            payload.setBusinessId("evidence-" + evidence.getEvidenceId());
            payload.getProperties().put("status", CourtEvidenceStatus.INVALID.getCode());
            graphEventPublisher.publish(tenantId, evidence.getCaseId(), null, CourtGraphEventType.INVALIDATE_NODE, payload);
        }
    }

    private KbDocumentEntity requireAllowedDocument(AuthPrincipal principal, Long documentId) {
        KbDocumentEntity document = kbDocumentMapper.selectOne(new LambdaQueryWrapper<KbDocumentEntity>()
                .eq(KbDocumentEntity::getTenantId, principal.tenantId())
                .eq(KbDocumentEntity::getOwnerUserId, principal.userId())
                .eq(KbDocumentEntity::getDocumentId, documentId)
                .ne(KbDocumentEntity::getStatus, KbDocumentStatus.DELETED)
                .last("limit 1"));
        if (document == null || !ALLOWED_DOCUMENT_BIZ_TYPES.contains(document.getBizType())) {
            throw AppException.notFound("文档不存在");
        }
        return document;
    }

    private void addEvidence(CourtCaseEntity courtCase, KbDocumentEntity document) {
        CourtCaseEvidenceEntity evidence = new CourtCaseEvidenceEntity();
        evidence.setTenantId(courtCase.getTenantId());
        evidence.setCaseId(courtCase.getCaseId());
        evidence.setDocumentId(document.getDocumentId());
        evidence.setDisplayName(document.getTitle());
        evidence.setStatus(CourtEvidenceStatus.ACTIVE);
        evidence.setCreatedAt(LocalDateTime.now());
        evidence.setUpdatedAt(LocalDateTime.now());
        courtCaseEvidenceMapper.insert(evidence);
    }

    private void publishCaseNode(CourtCaseEntity entity) {
        CourtGraphEventPayload payload = new CourtGraphEventPayload();
        payload.setLabel("Case");
        payload.setBusinessId("case-" + entity.getCaseId());
        payload.getProperties().put("title", entity.getTitle());
        payload.getProperties().put("status", entity.getStatus().getCode());
        payload.getProperties().put("userSide", entity.getUserSide() == null ? null : entity.getUserSide().getCode());
        graphEventPublisher.publish(entity.getTenantId(), entity.getCaseId(), null, CourtGraphEventType.UPSERT_NODE, payload);
    }
}
