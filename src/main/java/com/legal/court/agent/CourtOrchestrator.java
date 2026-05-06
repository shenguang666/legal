package com.legal.court.agent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.legal.common.AppException;
import com.legal.court.entity.CourtHearingRoundEntity;
import com.legal.court.mapper.CourtHearingRoundMapper;
import com.legal.enums.CourtHearingState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 智能小法庭庭审编排器。
 */
@Service
@Slf4j
public class CourtOrchestrator {

    private final CourtHearingRoundMapper courtHearingRoundMapper;

    public CourtOrchestrator(CourtHearingRoundMapper courtHearingRoundMapper) {
        this.courtHearingRoundMapper = courtHearingRoundMapper;
    }

    /**
     * 将待执行轮次按乐观锁切换为运行中，并分配新的 attemptId。
     */
    @Transactional
    public CourtHearingRoundEntity startRound(Long tenantId, Long caseId, Long roundId) {
        CourtHearingRoundEntity round = requireRound(tenantId, caseId, roundId);
        if (round.getState() != CourtHearingState.PENDING) {
            throw AppException.badRequest("庭审轮次不是待执行状态");
        }
        int nextAttemptId = (round.getAttemptId() == null ? 0 : round.getAttemptId()) + 1;
        int updated = courtHearingRoundMapper.markRunning(
                tenantId,
                caseId,
                roundId,
                round.getLockVersion() == null ? 0 : round.getLockVersion(),
                nextAttemptId
        );
        if (updated != 1) {
            throw AppException.badRequest("庭审轮次已被其他请求启动，请勿重复操作");
        }
        log.info("round.run tenantId={} caseId={} roundId={} attemptId={} status=RUNNING", tenantId, caseId, roundId, nextAttemptId);
        return requireRound(tenantId, caseId, roundId);
    }

    /**
     * 用户主动停止当前运行中的庭审轮次。
     */
    @Transactional
    public void stopRound(Long tenantId, Long caseId, Long roundId) {
        int updated = courtHearingRoundMapper.cancelRunning(tenantId, caseId, roundId, "用户主动停止当前庭审轮次");
        if (updated == 0) {
            throw AppException.badRequest("当前庭审轮次不在运行中，无法停止");
        }
    }

    private CourtHearingRoundEntity requireRound(Long tenantId, Long caseId, Long roundId) {
        CourtHearingRoundEntity round = courtHearingRoundMapper.selectOne(new LambdaQueryWrapper<CourtHearingRoundEntity>()
                .eq(CourtHearingRoundEntity::getTenantId, tenantId)
                .eq(CourtHearingRoundEntity::getCaseId, caseId)
                .eq(CourtHearingRoundEntity::getRoundId, roundId)
                .last("limit 1"));
        if (round == null) {
            throw AppException.notFound("庭审轮次不存在");
        }
        return round;
    }
}
