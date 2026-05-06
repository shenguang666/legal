package com.legal.court.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.legal.court.dto.CourtEvidenceAllowedRefs;
import com.legal.court.entity.CourtCaseEvidenceEntity;
import com.legal.court.mapper.CourtCaseEvidenceMapper;
import com.legal.enums.CourtEvidenceStatus;
import com.legal.enums.KbChunkType;
import com.legal.knowledge.entity.KbChunkEntity;
import com.legal.knowledge.mapper.KbChunkMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 智能小法庭证据引用服务。
 */
@Service
public class CourtEvidenceService {

    private final CourtCaseEvidenceMapper courtCaseEvidenceMapper;
    private final KbChunkMapper kbChunkMapper;

    public CourtEvidenceService(CourtCaseEvidenceMapper courtCaseEvidenceMapper, KbChunkMapper kbChunkMapper) {
        this.courtCaseEvidenceMapper = courtCaseEvidenceMapper;
        this.kbChunkMapper = kbChunkMapper;
    }

    /**
     * 按租户、案件、轮次生成本轮允许 AI 引用的 evidenceId、parentChunkId 与 childChunkId 白名单。
     */
    public CourtEvidenceAllowedRefs allowedRefsForRound(Long caseId, Long tenantId, Long roundId) {
        CourtEvidenceAllowedRefs result = new CourtEvidenceAllowedRefs();
        result.setTenantId(tenantId);
        result.setCaseId(caseId);
        result.setRoundId(roundId);
        if (caseId == null || tenantId == null) {
            result.setRefs(Collections.emptyList());
            return result;
        }

        List<CourtCaseEvidenceEntity> evidences = courtCaseEvidenceMapper.selectList(new LambdaQueryWrapper<CourtCaseEvidenceEntity>()
                .eq(CourtCaseEvidenceEntity::getTenantId, tenantId)
                .eq(CourtCaseEvidenceEntity::getCaseId, caseId)
                .eq(CourtCaseEvidenceEntity::getStatus, CourtEvidenceStatus.ACTIVE));
        if (evidences.isEmpty()) {
            result.setRefs(Collections.emptyList());
            return result;
        }

        Map<Long, Long> documentEvidenceMap = new LinkedHashMap<>();
        for (CourtCaseEvidenceEntity evidence : evidences) {
            result.getEvidenceIds().add(evidence.getEvidenceId());
            if (evidence.getDocumentId() != null) {
                documentEvidenceMap.putIfAbsent(evidence.getDocumentId(), evidence.getEvidenceId());
            }
        }
        if (documentEvidenceMap.isEmpty()) {
            result.setRefs(Collections.emptyList());
            return result;
        }

        List<KbChunkEntity> chunks = kbChunkMapper.selectCourtAllowedChunksByDocuments(tenantId, new ArrayList<>(documentEvidenceMap.keySet()));
        List<CourtEvidenceAllowedRefs.RefItem> refs = new ArrayList<>();
        for (KbChunkEntity chunk : chunks) {
            Long evidenceId = documentEvidenceMap.get(chunk.getDocumentId());
            if (evidenceId == null) {
                continue;
            }
            CourtEvidenceAllowedRefs.RefItem item = toRefItem(evidenceId, chunk);
            refs.add(item);
            result.getChildChunkIds().add(item.getChildChunkId());
            result.getParentChunkIds().add(item.getParentChunkId());
        }
        result.getParentChunkIds().removeIf(Objects::isNull);
        result.getChildChunkIds().removeIf(Objects::isNull);
        result.setRefs(refs);
        return result;
    }

    private CourtEvidenceAllowedRefs.RefItem toRefItem(Long evidenceId, KbChunkEntity chunk) {
        KbChunkType chunkType = chunk.getChunkType() == null ? KbChunkType.NORMAL : chunk.getChunkType();
        CourtEvidenceAllowedRefs.RefItem item = new CourtEvidenceAllowedRefs.RefItem();
        item.setEvidenceId(evidenceId);
        item.setDocumentId(chunk.getDocumentId());
        item.setChildChunkId(chunk.getChunkId());
        item.setParentChunkId(chunkType == KbChunkType.CHILD ? chunk.getParentChunkId() : chunk.getChunkId());
        item.setChunkType(chunkType.name());
        return item;
    }
}
