package com.legal.contract.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.legal.common.AppException;
import com.legal.contract.entity.ContractReviewEntity;
import com.legal.contract.entity.ContractReviewFieldEntity;
import com.legal.contract.entity.ContractReviewTaskEntity;
import com.legal.contract.entity.ContractRiskItemEntity;
import com.legal.contract.mapper.ContractReviewFieldMapper;
import com.legal.contract.mapper.ContractReviewMapper;
import com.legal.contract.mapper.ContractRiskItemMapper;
import com.legal.enums.ContractFieldStatus;
import com.legal.enums.ContractReviewStatus;
import com.legal.enums.ContractRiskLevel;
import com.legal.enums.ContractRiskSeverity;
import com.legal.enums.ContractRuleExecutionStatus;
import com.legal.knowledge.entity.KbChunkEntity;
import com.legal.knowledge.mapper.KbChunkMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ContractReviewProcessingService {

    private final ContractReviewMapper contractReviewMapper;
    private final ContractReviewFieldMapper contractReviewFieldMapper;
    private final ContractRiskItemMapper contractRiskItemMapper;
    private final KbChunkMapper kbChunkMapper;
    private final ContractFieldExtractionService fieldExtractionService;
    private final ContractRuleValidationService ruleValidationService;

    public ContractReviewProcessingService(ContractReviewMapper contractReviewMapper,
                                           ContractReviewFieldMapper contractReviewFieldMapper,
                                           ContractRiskItemMapper contractRiskItemMapper,
                                           KbChunkMapper kbChunkMapper,
                                           ContractFieldExtractionService fieldExtractionService,
                                           ContractRuleValidationService ruleValidationService) {
        this.contractReviewMapper = contractReviewMapper;
        this.contractReviewFieldMapper = contractReviewFieldMapper;
        this.contractRiskItemMapper = contractRiskItemMapper;
        this.kbChunkMapper = kbChunkMapper;
        this.fieldExtractionService = fieldExtractionService;
        this.ruleValidationService = ruleValidationService;
    }

    @Transactional
    public void processTask(ContractReviewTaskEntity task) {
        ContractReviewEntity review = contractReviewMapper.selectById(task.getReviewId());
        if (review == null) {
            throw AppException.notFound("合同审阅记录不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        review.setStatus(ContractReviewStatus.PROCESSING);
        review.setStartedAt(now);
        review.setFailureReason(null);
        review.setUpdatedAt(now);
        contractReviewMapper.updateById(review);

        contractReviewFieldMapper.delete(new LambdaQueryWrapper<ContractReviewFieldEntity>()
                .eq(ContractReviewFieldEntity::getReviewId, review.getReviewId()));
        contractRiskItemMapper.delete(new LambdaQueryWrapper<ContractRiskItemEntity>()
                .eq(ContractRiskItemEntity::getReviewId, review.getReviewId()));

        List<KbChunkEntity> chunks = kbChunkMapper.selectByDocVersion(task.getTenantId(), task.getDocumentId(), task.getDocVersion());
        if (chunks.isEmpty()) {
            throw AppException.badRequest("文档缺少可分析内容，无法执行合同审阅");
        }

        List<ExtractedContractField> extractedFields = fieldExtractionService.extract(task.getTenantId(), chunks);
        persistFields(review.getReviewId(), extractedFields, now);

        List<ContractRuleEvaluation> evaluations = ruleValidationService.evaluate(task.getTenantId(), chunks, extractedFields);
        persistRiskItems(review.getReviewId(), evaluations, now);

        long extractedCount = extractedFields.stream().filter(field -> field.getStatus() == ContractFieldStatus.EXTRACTED).count();
        long missingCount = extractedFields.stream().filter(field -> field.getStatus() == ContractFieldStatus.MISSING).count();
        long hitCount = evaluations.stream().filter(item -> item.getExecutionStatus() == ContractRuleExecutionStatus.HIT).count();
        review.setStatus(ContractReviewStatus.COMPLETED);
        review.setTotalFieldCount(extractedFields.size());
        review.setExtractedFieldCount((int) extractedCount);
        review.setMissingFieldCount((int) missingCount);
        review.setRiskCount((int) hitCount);
        review.setHitRuleCount((int) hitCount);
        review.setRiskLevel(resolveRiskLevel(evaluations));
        review.setSummaryText(buildSummary(extractedFields, evaluations));
        review.setCompletedAt(LocalDateTime.now());
        review.setUpdatedAt(LocalDateTime.now());
        contractReviewMapper.updateById(review);
    }

    public void markReviewFailed(ContractReviewTaskEntity task, String reason) {
        ContractReviewEntity review = contractReviewMapper.selectById(task.getReviewId());
        if (review == null) {
            return;
        }
        review.setStatus(ContractReviewStatus.FAILED);
        review.setFailureReason(truncate(reason));
        review.setUpdatedAt(LocalDateTime.now());
        contractReviewMapper.updateById(review);
    }

    private void persistFields(Long reviewId, List<ExtractedContractField> fields, LocalDateTime now) {
        for (ExtractedContractField field : fields) {
            ContractReviewFieldEntity entity = new ContractReviewFieldEntity();
            entity.setReviewId(reviewId);
            entity.setFieldCode(field.getFieldCode());
            entity.setFieldName(field.getFieldName());
            entity.setRawValue(field.getRawValue());
            entity.setNormalizedValue(field.getNormalizedValue());
            entity.setStatus(field.getStatus());
            entity.setConfidence(field.getConfidence());
            entity.setEvidenceText(field.getEvidenceText());
            entity.setSourceChunkRef(field.getSourceChunkRef());
            entity.setExtractorType(field.getExtractorType());
            entity.setFieldOrder(field.getFieldOrder());
            entity.setGroupKey(field.getGroupKey());
            entity.setExplanation(field.getExplanation());
            entity.setCreatedAt(now);
            contractReviewFieldMapper.insert(entity);
        }
    }

    private void persistRiskItems(Long reviewId, List<ContractRuleEvaluation> evaluations, LocalDateTime now) {
        for (ContractRuleEvaluation evaluation : evaluations) {
            ContractRiskItemEntity entity = new ContractRiskItemEntity();
            entity.setReviewId(reviewId);
            entity.setRuleCode(evaluation.getRuleCode());
            entity.setRuleName(evaluation.getRuleName());
            entity.setRuleType(evaluation.getRuleType());
            entity.setSeverity(evaluation.getSeverity());
            entity.setExecutionStatus(evaluation.getExecutionStatus());
            entity.setMessage(evaluation.getMessage());
            entity.setEvidenceText(evaluation.getEvidenceText());
            entity.setAffectedFieldCodes(evaluation.getAffectedFieldCodes());
            entity.setCreatedAt(now);
            contractRiskItemMapper.insert(entity);
        }
    }

    private ContractRiskLevel resolveRiskLevel(List<ContractRuleEvaluation> evaluations) {
        List<ContractRiskSeverity> hitSeverities = evaluations.stream()
                .filter(item -> item.getExecutionStatus() == ContractRuleExecutionStatus.HIT)
                .map(ContractRuleEvaluation::getSeverity)
                .sorted(Comparator.comparingInt(this::severityOrder).reversed())
                .toList();
        if (hitSeverities.isEmpty()) {
            return ContractRiskLevel.LOW;
        }
        ContractRiskSeverity highest = hitSeverities.get(0);
        if (highest == ContractRiskSeverity.HIGH) {
            return ContractRiskLevel.HIGH;
        }
        if (highest == ContractRiskSeverity.MEDIUM) {
            return ContractRiskLevel.MEDIUM;
        }
        return ContractRiskLevel.LOW;
    }

    private int severityOrder(ContractRiskSeverity severity) {
        if (severity == ContractRiskSeverity.HIGH) {
            return 3;
        }
        if (severity == ContractRiskSeverity.MEDIUM) {
            return 2;
        }
        return 1;
    }

    private String buildSummary(List<ExtractedContractField> fields, List<ContractRuleEvaluation> evaluations) {
        List<String> hitMessages = new ArrayList<>();
        for (ContractRuleEvaluation evaluation : evaluations) {
            if (evaluation.getExecutionStatus() == ContractRuleExecutionStatus.HIT && StringUtils.hasText(evaluation.getMessage())) {
                hitMessages.add(evaluation.getMessage());
            }
        }
        if (hitMessages.isEmpty()) {
            return "已提取 " + fields.stream().filter(field -> field.getStatus() == ContractFieldStatus.EXTRACTED).count()
                    + "/" + fields.size() + " 个字段，未识别到明显业务风险";
        }
        return String.join("；", hitMessages.stream().limit(3).toList());
    }

    private String truncate(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }
}
