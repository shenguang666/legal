package com.legal.court.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legal.common.AppException;
import com.legal.config.OpenAiChatModelProperties;
import com.legal.court.dto.CourtSuggestionGap;
import com.legal.court.entity.CourtSupplementSuggestionEntity;
import com.legal.court.graph.CourtSuggestionRuleEngine;
import com.legal.court.mapper.CourtSupplementSuggestionMapper;
import com.legal.court.observability.CourtMetricsService;
import com.legal.enums.CourtSuggestionSeverity;
import com.legal.enums.CourtSuggestionStatus;
import com.legal.security.AuthPrincipal;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 智能小法庭补证建议服务。
 */
@Slf4j
@Service
public class CourtSuggestionService {

    private final CourtSuggestionRuleEngine ruleEngine;
    private final CourtCaseService courtCaseService;
    private final CourtSupplementSuggestionMapper suggestionMapper;
    private final ObjectMapper objectMapper;
    private final ChatModel chatModel;
    private final OpenAiChatModelProperties modelProperties;
    private final CourtMetricsService courtMetricsService;

    public CourtSuggestionService(CourtSuggestionRuleEngine ruleEngine,
                                  CourtCaseService courtCaseService,
                                  CourtSupplementSuggestionMapper suggestionMapper,
                                  ObjectMapper objectMapper,
                                  @Nullable ChatModel chatModel,
                                  OpenAiChatModelProperties modelProperties,
                                  CourtMetricsService courtMetricsService) {
        this.ruleEngine = ruleEngine;
        this.courtCaseService = courtCaseService;
        this.suggestionMapper = suggestionMapper;
        this.objectMapper = objectMapper;
        this.chatModel = chatModel;
        this.modelProperties = modelProperties;
        this.courtMetricsService = courtMetricsService;
    }

    /**
     * 刷新案件补证建议：规则先行，LLM 只改写解释，不得新增缺口。
     */
    @Transactional
    public List<CourtSupplementSuggestionEntity> refreshSuggestions(Long tenantId, Long caseId, Long roundId) {
        List<CourtSuggestionGap> gaps = ruleEngine.detectGaps(tenantId, caseId, 100);
        List<CourtSuggestionGap> rewritten = rewriteGaps(gaps);
        Map<String, CourtSupplementSuggestionEntity> existing = loadOpenSuggestions(tenantId, caseId);
        Set<String> currentKeys = rewritten.stream().map(this::gapKey).collect(Collectors.toSet());
        List<CourtSupplementSuggestionEntity> saved = new ArrayList<>();
        for (CourtSuggestionGap gap : rewritten) {
            String key = gapKey(gap);
            CourtSupplementSuggestionEntity entity = existing.get(key);
            if (entity == null) {
                entity = new CourtSupplementSuggestionEntity();
                entity.setTenantId(tenantId);
                entity.setCaseId(caseId);
                entity.setCreatedAt(LocalDateTime.now());
            }
            applyGap(entity, roundId, gap);
            if (entity.getSuggestionId() == null) {
                suggestionMapper.insert(entity);
            } else {
                suggestionMapper.updateById(entity);
            }
            saved.add(entity);
        }
        resolveMissing(existing, currentKeys);
        long highCount = saved.stream().filter(item -> item.getSeverity() == CourtSuggestionSeverity.HIGH).count();
        courtMetricsService.recordHighSuggestions(highCount);
        log.info("suggestion.refresh tenantId={} caseId={} roundId={} total={} highCount={}", tenantId, caseId, roundId, saved.size(), highCount);
        return saved;
    }

    /**
     * 查询案件补证建议列表。
     */
    public List<CourtSupplementSuggestionEntity> listSuggestions(AuthPrincipal principal, Long caseId, CourtSuggestionStatus status) {
        courtCaseService.requireCase(principal, caseId);
        LambdaQueryWrapper<CourtSupplementSuggestionEntity> wrapper = new LambdaQueryWrapper<CourtSupplementSuggestionEntity>()
                .eq(CourtSupplementSuggestionEntity::getTenantId, principal.tenantId())
                .eq(CourtSupplementSuggestionEntity::getCaseId, caseId)
                .orderByDesc(CourtSupplementSuggestionEntity::getSeverity)
                .orderByDesc(CourtSupplementSuggestionEntity::getUpdatedAt);
        if (status != null) {
            wrapper.eq(CourtSupplementSuggestionEntity::getStatus, status);
        }
        return suggestionMapper.selectList(wrapper);
    }

    /**
     * 忽略补证建议。
     */
    @Transactional
    public void ignoreSuggestion(AuthPrincipal principal, Long caseId, Long suggestionId) {
        updateSuggestionStatus(principal, caseId, suggestionId, CourtSuggestionStatus.IGNORED);
    }

    /**
     * 手动解除补证建议。
     */
    @Transactional
    public void resolveSuggestion(AuthPrincipal principal, Long caseId, Long suggestionId) {
        updateSuggestionStatus(principal, caseId, suggestionId, CourtSuggestionStatus.RESOLVED);
    }

    private void updateSuggestionStatus(AuthPrincipal principal, Long caseId, Long suggestionId, CourtSuggestionStatus status) {
        courtCaseService.requireCase(principal, caseId);
        CourtSupplementSuggestionEntity entity = suggestionMapper.selectOne(new LambdaQueryWrapper<CourtSupplementSuggestionEntity>()
                .eq(CourtSupplementSuggestionEntity::getTenantId, principal.tenantId())
                .eq(CourtSupplementSuggestionEntity::getCaseId, caseId)
                .eq(CourtSupplementSuggestionEntity::getSuggestionId, suggestionId)
                .last("limit 1"));
        if (entity == null) {
            throw AppException.notFound("补证建议不存在");
        }
        entity.setStatus(status);
        entity.setUpdatedAt(LocalDateTime.now());
        suggestionMapper.updateById(entity);
    }

    private List<CourtSuggestionGap> rewriteGaps(List<CourtSuggestionGap> gaps) {
        if (gaps == null || gaps.isEmpty() || chatModel == null) {
            return gaps == null ? List.of() : gaps;
        }
        try {
            String response = chatModel.chat(List.of(
                    SystemMessage.from("你是补证建议文案改写器，只能基于输入 JSON 改写 rationale、missingDescription、recommendedMaterials，禁止新增、删除或改变 type 和业务ID。只返回 JSON 数组。"),
                    UserMessage.from(toJson(gaps))
            )).aiMessage().text();
            return filterLlmRewrite(gaps, response);
        } catch (Exception ex) {
            log.warn("智能小法庭补证建议 LLM 改写失败，保留规则结果 model={} error={}", modelProperties.getModelName(), ex.getMessage());
            return gaps;
        }
    }

    private List<CourtSuggestionGap> filterLlmRewrite(List<CourtSuggestionGap> original, String response) throws Exception {
        Map<String, CourtSuggestionGap> originalByKey = original.stream().collect(Collectors.toMap(this::gapKey, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        JsonNode root = objectMapper.readTree(response);
        if (!root.isArray()) {
            return original;
        }
        List<CourtSuggestionGap> result = new ArrayList<>();
        for (JsonNode node : root) {
            CourtSuggestionGap candidate = objectMapper.convertValue(node, CourtSuggestionGap.class);
            CourtSuggestionGap source = originalByKey.get(gapKey(candidate));
            if (source == null) {
                continue;
            }
            source.setMissingDescription(StringUtils.hasText(candidate.getMissingDescription()) ? candidate.getMissingDescription() : source.getMissingDescription());
            source.setRationale(StringUtils.hasText(candidate.getRationale()) ? candidate.getRationale() : source.getRationale());
            if (candidate.getRecommendedMaterials() != null && !candidate.getRecommendedMaterials().isEmpty()) {
                source.setRecommendedMaterials(candidate.getRecommendedMaterials());
            }
            result.add(source);
        }
        return result.isEmpty() ? original : result;
    }

    private Map<String, CourtSupplementSuggestionEntity> loadOpenSuggestions(Long tenantId, Long caseId) {
        List<CourtSupplementSuggestionEntity> rows = suggestionMapper.selectList(new LambdaQueryWrapper<CourtSupplementSuggestionEntity>()
                .eq(CourtSupplementSuggestionEntity::getTenantId, tenantId)
                .eq(CourtSupplementSuggestionEntity::getCaseId, caseId)
                .eq(CourtSupplementSuggestionEntity::getStatus, CourtSuggestionStatus.OPEN));
        Map<String, CourtSupplementSuggestionEntity> result = new LinkedHashMap<>();
        for (CourtSupplementSuggestionEntity row : rows) {
            result.put(entityKey(row), row);
        }
        return result;
    }

    private void resolveMissing(Map<String, CourtSupplementSuggestionEntity> existing, Set<String> currentKeys) {
        for (Map.Entry<String, CourtSupplementSuggestionEntity> entry : existing.entrySet()) {
            if (currentKeys.contains(entry.getKey())) {
                continue;
            }
            CourtSupplementSuggestionEntity entity = entry.getValue();
            entity.setStatus(CourtSuggestionStatus.RESOLVED);
            entity.setUpdatedAt(LocalDateTime.now());
            suggestionMapper.updateById(entity);
        }
    }

    private void applyGap(CourtSupplementSuggestionEntity entity, Long roundId, CourtSuggestionGap gap) {
        entity.setRoundId(roundId);
        entity.setSuggestionType(gap.getType());
        entity.setSeverity(gap.getSeverity());
        entity.setStatus(CourtSuggestionStatus.OPEN);
        entity.setAffectedClaimIdsJson(toJson(listOfNonBlank(gap.getClaimBusinessId())));
        entity.setAffectedEvidenceIdsJson("[]");
        entity.setMissingDescription(gap.getMissingDescription());
        entity.setRecommendedMaterialsJson(toJson(gap.getRecommendedMaterials()));
        entity.setRationale(gap.getRationale());
        entity.setPotentialImpact(buildPotentialImpact(gap));
        entity.setGraphGapBusinessId(StringUtils.hasText(gap.getGapOrRiskBusinessId()) ? gap.getGapOrRiskBusinessId() : gap.getDefenseBusinessId());
        entity.setUpdatedAt(LocalDateTime.now());
    }

    private String buildPotentialImpact(CourtSuggestionGap gap) {
        return switch (gap.getSeverity()) {
            case HIGH -> "该缺口可能直接影响核心诉求或关键抗辩的成立";
            case MEDIUM -> "该缺口可能削弱证据链完整性或证明力";
            case LOW -> "该缺口主要影响庭审表达完整性";
        };
    }

    private String gapKey(CourtSuggestionGap gap) {
        return gap.getType().getCode() + "|" + safe(gap.getClaimBusinessId()) + "|" + safe(anchorBusinessId(gap));
    }

    private String entityKey(CourtSupplementSuggestionEntity entity) {
        List<String> claimIds = fromJsonList(entity.getAffectedClaimIdsJson());
        String claimId = claimIds.isEmpty() ? "" : claimIds.get(0);
        return entity.getSuggestionType().getCode() + "|" + claimId + "|" + safe(entity.getGraphGapBusinessId());
    }

    private String anchorBusinessId(CourtSuggestionGap gap) {
        if (StringUtils.hasText(gap.getGapOrRiskBusinessId())) {
            return gap.getGapOrRiskBusinessId();
        }
        if (StringUtils.hasText(gap.getDefenseBusinessId())) {
            return gap.getDefenseBusinessId();
        }
        return "";
    }

    private List<String> fromJsonList(String json) {
        try {
            return StringUtils.hasText(json) ? objectMapper.readValue(json, new TypeReference<List<String>>() { }) : List.of();
        } catch (Exception ex) {
            return List.of();
        }
    }

    private List<String> listOfNonBlank(String value) {
        return StringUtils.hasText(value) ? List.of(value) : List.of();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (Exception ex) {
            throw new IllegalStateException("补证建议 JSON 序列化失败", ex);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
