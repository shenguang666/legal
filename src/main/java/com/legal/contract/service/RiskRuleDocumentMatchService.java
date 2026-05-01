package com.legal.contract.service;

import com.legal.config.ContractReviewProperties;
import com.legal.contract.entity.ContractRuleDefinitionEntity;
import com.legal.enums.ContractRuleExecutionStatus;
import com.legal.retrieval.service.ChunkSearchHit;
import com.legal.retrieval.service.ElasticsearchChunkStore;
import com.legal.retrieval.service.OpenAiEmbeddingClient;
import com.legal.knowledge.entity.KbChunkEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RiskRuleDocumentMatchService {

    private final OpenAiEmbeddingClient embeddingClient;
    private final ElasticsearchChunkStore elasticsearchChunkStore;
    private final ContractReviewProperties properties;

    public RiskRuleDocumentMatchService(OpenAiEmbeddingClient embeddingClient,
                                        ElasticsearchChunkStore elasticsearchChunkStore,
                                        ContractReviewProperties properties) {
        this.embeddingClient = embeddingClient;
        this.elasticsearchChunkStore = elasticsearchChunkStore;
        this.properties = properties;
    }

    public List<ContractRuleEvaluation> match(Long tenantId,
                                              List<KbChunkEntity> reviewChunks,
                                              List<ContractRuleDefinitionEntity> retrievalRules,
                                              String riskRuleIndexName) {
        if (reviewChunks == null || reviewChunks.isEmpty() || retrievalRules == null || retrievalRules.isEmpty()) {
            return List.of();
        }
        if (!elasticsearchChunkStore.isEnabled() || !properties.getRiskRuleRetrieval().isEnabled()) {
            return List.of();
        }
        Map<Long, List<ContractRuleDefinitionEntity>> rulesByDocumentId = new LinkedHashMap<>();
        for (ContractRuleDefinitionEntity rule : retrievalRules) {
            if (rule.getDocumentId() == null) {
                continue;
            }
            rulesByDocumentId.computeIfAbsent(rule.getDocumentId(), ignored -> new ArrayList<>()).add(rule);
        }
        if (rulesByDocumentId.isEmpty()) {
            return List.of();
        }

        Map<Long, MatchHolder> bestMatches = new LinkedHashMap<>();
        int topK = Math.max(1, properties.getRiskRuleRetrieval().getTopK());
        for (KbChunkEntity chunk : reviewChunks) {
            List<Float> vector = embeddingClient.embed(chunk.getContent());
            List<ChunkSearchHit> hits = elasticsearchChunkStore.vectorSearch(tenantId, vector, topK, riskRuleIndexName);
            for (ChunkSearchHit hit : hits) {
                List<ContractRuleDefinitionEntity> rules = rulesByDocumentId.get(hit.getDocumentId());
                if (rules == null || rules.isEmpty()) {
                    continue;
                }
                for (ContractRuleDefinitionEntity rule : rules) {
                    double threshold = rule.getHitThreshold() == null
                            ? properties.getRiskRuleRetrieval().getMinScore()
                            : rule.getHitThreshold().doubleValue();
                    if (hit.getScore() < threshold) {
                        continue;
                    }
                    MatchHolder current = bestMatches.get(rule.getRuleId());
                    if (current == null || hit.getScore() > current.hit().getScore()) {
                        bestMatches.put(rule.getRuleId(), new MatchHolder(rule, hit, chunk));
                    }
                }
            }
        }

        return bestMatches.values().stream()
                .sorted(Comparator.comparingDouble((MatchHolder item) -> item.hit().getScore()).reversed())
                .limit(Math.max(1, properties.getRiskRuleRetrieval().getMaxHitsPerReview()))
                .map(this::toEvaluation)
                .toList();
    }

    private ContractRuleEvaluation toEvaluation(MatchHolder match) {
        ContractRuleEvaluation evaluation = new ContractRuleEvaluation();
        evaluation.setRuleCode(match.rule().getRuleCode());
        evaluation.setRuleName(match.rule().getRuleName());
        evaluation.setRuleType(match.rule().getRuleType());
        evaluation.setSeverity(match.rule().getSeverity());
        evaluation.setExecutionStatus(ContractRuleExecutionStatus.HIT);
        evaluation.setAffectedFieldCodes(match.rule().getFieldCode());
        evaluation.setMessage("命中风险规则：" + match.rule().getRuleName());
        evaluation.setEvidenceText(buildEvidence(match.reviewChunk().getContent(), match.hit().getContent()));
        return evaluation;
    }

    private String buildEvidence(String reviewContent, String ruleContent) {
        int max = Math.max(60, properties.getExtraction().getMaxEvidenceLength());
        String left = truncate("审查片段：" + normalize(reviewContent), max / 2);
        String right = truncate("规则片段：" + normalize(ruleContent), max / 2);
        if (!StringUtils.hasText(left)) {
            return right;
        }
        if (!StringUtils.hasText(right)) {
            return left;
        }
        return left + " || " + right;
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
    }

    private String truncate(String value, int max) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private record MatchHolder(ContractRuleDefinitionEntity rule, ChunkSearchHit hit, KbChunkEntity reviewChunk) {
    }
}
