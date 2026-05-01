package com.legal.contract.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.legal.common.AppException;
import com.legal.contract.dto.ContractReviewDetailDto;
import com.legal.contract.dto.ContractReviewFieldDto;
import com.legal.contract.dto.ContractReviewTriggerRequest;
import com.legal.contract.dto.ContractRiskItemDto;
import com.legal.contract.entity.ContractReviewEntity;
import com.legal.contract.entity.ContractReviewFieldEntity;
import com.legal.contract.entity.ContractReviewTaskEntity;
import com.legal.contract.entity.ContractRiskItemEntity;
import com.legal.contract.mapper.ContractReviewFieldMapper;
import com.legal.contract.mapper.ContractReviewMapper;
import com.legal.contract.mapper.ContractReviewTaskMapper;
import com.legal.contract.mapper.ContractRiskItemMapper;
import com.legal.enums.ContractReviewStatus;
import com.legal.enums.ContractReviewTaskStatus;
import com.legal.enums.ContractRiskLevel;
import com.legal.enums.KbDocumentBizType;
import com.legal.enums.KbDocumentStatus;
import com.legal.knowledge.entity.KbDocumentEntity;
import com.legal.knowledge.mapper.KbDocumentMapper;
import com.legal.security.AuthPrincipal;
import com.legal.security.IdempotencyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ContractReviewService {

    private final ContractReviewMapper contractReviewMapper;
    private final ContractReviewFieldMapper contractReviewFieldMapper;
    private final ContractRiskItemMapper contractRiskItemMapper;
    private final ContractReviewTaskMapper contractReviewTaskMapper;
    private final KbDocumentMapper kbDocumentMapper;
    private final IdempotencyService idempotencyService;

    public ContractReviewService(ContractReviewMapper contractReviewMapper,
                                 ContractReviewFieldMapper contractReviewFieldMapper,
                                 ContractRiskItemMapper contractRiskItemMapper,
                                 ContractReviewTaskMapper contractReviewTaskMapper,
                                 KbDocumentMapper kbDocumentMapper,
                                 IdempotencyService idempotencyService) {
        this.contractReviewMapper = contractReviewMapper;
        this.contractReviewFieldMapper = contractReviewFieldMapper;
        this.contractRiskItemMapper = contractRiskItemMapper;
        this.contractReviewTaskMapper = contractReviewTaskMapper;
        this.kbDocumentMapper = kbDocumentMapper;
        this.idempotencyService = idempotencyService;
    }

    @Transactional
    public ContractReviewDetailDto triggerReview(AuthPrincipal principal, ContractReviewTriggerRequest request) {
        idempotencyService.ensureUnique(principal, "contract-review:create", request.getRequestId());
        KbDocumentEntity document = requireDocument(principal, request.getDocumentId());
        ContractReviewEntity latest = contractReviewMapper.selectLatestByDocument(principal.tenantId(), request.getDocumentId());
        if (latest != null && document.getDocVersion().equals(latest.getDocVersion()) && latest.getStatus() != ContractReviewStatus.FAILED) {
            return toDetail(latest);
        }
        return toDetail(createReview(principal, document));
    }

    @Transactional
    public ContractReviewDetailDto rerunReview(AuthPrincipal principal, Long reviewId, String requestId) {
        idempotencyService.ensureUnique(principal, "contract-review:rerun", requestId);
        ContractReviewEntity existing = requireReview(principal, reviewId);
        KbDocumentEntity document = requireDocument(principal, existing.getDocumentId());
        return toDetail(createReview(principal, document));
    }

    public ContractReviewDetailDto getReviewDetail(AuthPrincipal principal, Long reviewId) {
        return toDetail(requireReview(principal, reviewId));
    }

    public ContractReviewDetailDto getLatestReviewByDocument(AuthPrincipal principal, Long documentId) {
        requireDocument(principal, documentId);
        ContractReviewEntity latest = contractReviewMapper.selectLatestByDocument(principal.tenantId(), documentId);
        return latest == null ? null : toDetail(latest);
    }

    private ContractReviewEntity createReview(AuthPrincipal principal, KbDocumentEntity document) {
        LocalDateTime now = LocalDateTime.now();
        ContractReviewEntity review = new ContractReviewEntity();
        review.setTenantId(principal.tenantId());
        review.setDocumentId(document.getDocumentId());
        review.setDocVersion(document.getDocVersion());
        review.setOwnerUserId(document.getOwnerUserId());
        review.setTriggeredByUserId(principal.userId());
        review.setStatus(ContractReviewStatus.PENDING);
        review.setRiskLevel(ContractRiskLevel.LOW);
        review.setRiskCount(0);
        review.setHitRuleCount(0);
        review.setTotalFieldCount(0);
        review.setExtractedFieldCount(0);
        review.setMissingFieldCount(0);
        review.setCreatedAt(now);
        review.setUpdatedAt(now);
        contractReviewMapper.insert(review);

        ContractReviewTaskEntity task = new ContractReviewTaskEntity();
        task.setTenantId(principal.tenantId());
        task.setReviewId(review.getReviewId());
        task.setDocumentId(document.getDocumentId());
        task.setDocVersion(document.getDocVersion());
        task.setStatus(ContractReviewTaskStatus.PENDING);
        task.setRetryCount(0);
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        contractReviewTaskMapper.insert(task);
        return review;
    }

    private ContractReviewEntity requireReview(AuthPrincipal principal, Long reviewId) {
        ContractReviewEntity review = contractReviewMapper.selectById(reviewId);
        if (review == null || !principal.tenantId().equals(review.getTenantId())) {
            throw AppException.notFound("合同审阅记录不存在");
        }
        requireDocument(principal, review.getDocumentId());
        return review;
    }

    private KbDocumentEntity requireDocument(AuthPrincipal principal, Long documentId) {
        KbDocumentEntity document = kbDocumentMapper.selectOne(new LambdaQueryWrapper<KbDocumentEntity>()
                .eq(KbDocumentEntity::getDocumentId, documentId)
                .eq(KbDocumentEntity::getTenantId, principal.tenantId())
                .eq(KbDocumentEntity::getOwnerUserId, principal.userId())
                .eq(KbDocumentEntity::getBizType, KbDocumentBizType.TIANYAN_REVIEW)
                .last("limit 1"));
        if (document == null || document.getStatus() == KbDocumentStatus.DELETED) {
            throw AppException.notFound("文档不存在");
        }
        return document;
    }

    private ContractReviewDetailDto toDetail(ContractReviewEntity review) {
        ContractReviewDetailDto dto = new ContractReviewDetailDto();
        dto.setReviewId(review.getReviewId());
        dto.setDocumentId(review.getDocumentId());
        dto.setDocVersion(review.getDocVersion());
        dto.setStatus(review.getStatus() == null ? null : review.getStatus().getCode());
        dto.setRiskLevel(review.getRiskLevel() == null ? null : review.getRiskLevel().getCode());
        dto.setRiskCount(review.getRiskCount());
        dto.setHitRuleCount(review.getHitRuleCount());
        dto.setTotalFieldCount(review.getTotalFieldCount());
        dto.setExtractedFieldCount(review.getExtractedFieldCount());
        dto.setMissingFieldCount(review.getMissingFieldCount());
        dto.setSummaryText(review.getSummaryText());
        dto.setFailureReason(review.getFailureReason());
        dto.setCreatedAt(review.getCreatedAt());
        dto.setUpdatedAt(review.getUpdatedAt());
        dto.setStartedAt(review.getStartedAt());
        dto.setCompletedAt(review.getCompletedAt());
        dto.setFields(contractReviewFieldMapper.selectByReviewId(review.getReviewId()).stream().map(this::toFieldDto).toList());
        dto.setRiskItems(contractRiskItemMapper.selectByReviewId(review.getReviewId()).stream().map(this::toRiskItemDto).toList());
        return dto;
    }

    private ContractReviewFieldDto toFieldDto(ContractReviewFieldEntity entity) {
        ContractReviewFieldDto dto = new ContractReviewFieldDto();
        dto.setFieldId(entity.getFieldId());
        dto.setFieldCode(entity.getFieldCode());
        dto.setFieldName(entity.getFieldName());
        dto.setRawValue(entity.getRawValue());
        dto.setNormalizedValue(entity.getNormalizedValue());
        dto.setStatus(entity.getStatus() == null ? null : entity.getStatus().getCode());
        dto.setConfidence(entity.getConfidence() == null ? null : entity.getConfidence().doubleValue());
        dto.setEvidenceText(entity.getEvidenceText());
        dto.setSourceChunkRef(entity.getSourceChunkRef());
        dto.setExtractorType(entity.getExtractorType());
        dto.setFieldOrder(entity.getFieldOrder());
        dto.setGroupKey(entity.getGroupKey());
        dto.setExplanation(entity.getExplanation());
        return dto;
    }

    private ContractRiskItemDto toRiskItemDto(ContractRiskItemEntity entity) {
        ContractRiskItemDto dto = new ContractRiskItemDto();
        dto.setRiskId(entity.getRiskId());
        dto.setRuleCode(entity.getRuleCode());
        dto.setRuleName(entity.getRuleName());
        dto.setRuleType(entity.getRuleType() == null ? null : entity.getRuleType().getCode());
        dto.setSeverity(entity.getSeverity() == null ? null : entity.getSeverity().getCode());
        dto.setExecutionStatus(entity.getExecutionStatus() == null ? null : entity.getExecutionStatus().getCode());
        dto.setMessage(entity.getMessage());
        dto.setEvidenceText(entity.getEvidenceText());
        dto.setAffectedFieldCodes(entity.getAffectedFieldCodes());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}
