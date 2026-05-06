package com.legal.court.agent;

import com.legal.common.AppException;
import com.legal.court.dto.CourtAgentEvidenceRef;
import com.legal.court.dto.CourtAgentResponse;
import com.legal.court.dto.CourtAgentValidationResult;
import com.legal.court.dto.CourtEvidenceAllowedRefs;
import com.legal.enums.CourtArgumentStance;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 智能小法庭 AI 输出证据引用三层校验器。
 */
@Component
public class CourtAgentResponseValidator {

    /**
     * 校验 AI 结构化输出，执行 schema、白名单与归属三层校验。
     */
    public CourtAgentValidationResult validate(CourtAgentResponse response, CourtEvidenceAllowedRefs allowedRefs) {
        validateSchema(response, allowedRefs);
        CourtAgentValidationResult result = new CourtAgentValidationResult();
        result.setContent(response.getContent());
        result.setRationale(response.getRationale());
        result.setStance(response.getStance());

        List<CourtAgentEvidenceRef> refs = response.getEvidenceRefs() == null ? List.of() : response.getEvidenceRefs();
        List<CourtAgentEvidenceRef> validRefs = new ArrayList<>();
        List<CourtAgentEvidenceRef> droppedRefs = new ArrayList<>();
        for (CourtAgentEvidenceRef ref : refs) {
            if (isAllowed(ref, allowedRefs)) {
                validRefs.add(ref);
            } else {
                droppedRefs.add(ref);
            }
        }

        result.setValidRefs(validRefs);
        result.setDroppedRefs(droppedRefs);
        result.setEvidenceVerifiedCount(validRefs.size());
        result.setEvidenceDroppedCount(droppedRefs.size());
        if (!droppedRefs.isEmpty() || requiresEvidence(response.getStance()) && validRefs.isEmpty()) {
            result.setStance(CourtArgumentStance.PENDING_PROOF);
            result.setDegradedToPendingProof(true);
            result.setFailureReason(buildFailureReason(validRefs.size(), droppedRefs.size()));
        }
        return result;
    }

    private void validateSchema(CourtAgentResponse response, CourtEvidenceAllowedRefs allowedRefs) {
        if (response == null) {
            throw AppException.badRequest("AI 角色输出不能为空");
        }
        if (!StringUtils.hasText(response.getContent())) {
            throw AppException.badRequest("AI 角色输出缺少 content 字段");
        }
        if (response.getStance() == null) {
            throw AppException.badRequest("AI 角色输出缺少 stance 字段");
        }
        if (allowedRefs == null) {
            throw AppException.badRequest("证据引用白名单不能为空");
        }
        List<CourtAgentEvidenceRef> refs = response.getEvidenceRefs();
        if (refs == null) {
            response.setEvidenceRefs(new ArrayList<>());
            return;
        }
        for (CourtAgentEvidenceRef ref : refs) {
            if (ref == null) {
                throw AppException.badRequest("AI 角色输出包含空证据引用");
            }
            if (ref.getEvidenceId() == null && ref.getParentChunkId() == null && ref.getChildChunkId() == null) {
                throw AppException.badRequest("AI 角色输出证据引用缺少 evidenceId 或 chunkId");
            }
        }
    }

    private boolean isAllowed(CourtAgentEvidenceRef ref, CourtEvidenceAllowedRefs allowedRefs) {
        boolean evidenceAllowed = ref.getEvidenceId() == null || allowedRefs.containsEvidenceId(ref.getEvidenceId());
        boolean parentAllowed = ref.getParentChunkId() == null || allowedRefs.containsChunkId(ref.getParentChunkId());
        boolean childAllowed = ref.getChildChunkId() == null || allowedRefs.containsChunkId(ref.getChildChunkId());
        return evidenceAllowed && parentAllowed && childAllowed;
    }

    private boolean requiresEvidence(CourtArgumentStance stance) {
        return stance == CourtArgumentStance.SUPPORT || stance == CourtArgumentStance.REBUT;
    }

    private String buildFailureReason(int validCount, int droppedCount) {
        if (validCount == 0) {
            return "未命中本案证据白名单，已降级为待证明";
        }
        return "存在 " + droppedCount + " 条非法证据引用，已丢弃并降级为待证明";
    }
}
