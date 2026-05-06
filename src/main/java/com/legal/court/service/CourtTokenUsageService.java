package com.legal.court.service;

import com.legal.court.dto.CourtRoleAgentResult;
import com.legal.court.entity.CourtHearingMessageEntity;
import com.legal.court.mapper.CourtCaseMapper;
import com.legal.court.mapper.CourtHearingMessageMapper;
import com.legal.court.mapper.CourtHearingRoundMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 智能小法庭 Token 消耗记录服务。
 */
@Service
public class CourtTokenUsageService {

    private final CourtHearingMessageMapper courtHearingMessageMapper;
    private final CourtHearingRoundMapper courtHearingRoundMapper;
    private final CourtCaseMapper courtCaseMapper;

    public CourtTokenUsageService(CourtHearingMessageMapper courtHearingMessageMapper,
                                  CourtHearingRoundMapper courtHearingRoundMapper,
                                  CourtCaseMapper courtCaseMapper) {
        this.courtHearingMessageMapper = courtHearingMessageMapper;
        this.courtHearingRoundMapper = courtHearingRoundMapper;
        this.courtCaseMapper = courtCaseMapper;
    }

    /**
     * 记录智能小法庭 Agent 输出消息，并累计轮次与案件 token 消耗。
     */
    @Transactional
    public CourtHearingMessageEntity recordAgentMessage(Long tenantId,
                                                        Long caseId,
                                                        Long roundId,
                                                        Integer attemptId,
                                                        CourtRoleAgentResult result) {
        int totalTokens = result == null || result.getTokenUsage() == null ? 0 : Math.max(0, result.getTokenUsage());
        CourtHearingMessageEntity message = new CourtHearingMessageEntity();
        message.setTenantId(tenantId);
        message.setCaseId(caseId);
        message.setRoundId(roundId);
        message.setAttemptId(attemptId);
        message.setSpeakerRole(result == null ? null : result.getSpeaker());
        message.setMessageType("AGENT_OUTPUT");
        message.setContent(result == null ? "" : result.getRawText());
        message.setTokenInput(0);
        message.setTokenOutput(totalTokens);
        message.setCreatedAt(LocalDateTime.now());
        courtHearingMessageMapper.insert(message);
        if (totalTokens > 0) {
            courtHearingRoundMapper.increaseTotalTokens(tenantId, caseId, roundId, totalTokens);
            courtCaseMapper.increaseTotalTokens(tenantId, caseId, totalTokens);
        }
        return message;
    }
}
