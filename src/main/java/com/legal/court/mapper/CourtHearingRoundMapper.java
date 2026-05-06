package com.legal.court.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.legal.court.entity.CourtHearingRoundEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 智能小法庭庭审轮次 Mapper。
 */
@Mapper
public interface CourtHearingRoundMapper extends BaseMapper<CourtHearingRoundEntity> {

    /**
     * 乐观锁切换庭审轮次为运行中。
     */
    @Update("""
            UPDATE court_hearing_round
            SET state = 'RUNNING',
                attempt_id = #{attemptId},
                lock_version = lock_version + 1,
                started_at = NOW(),
                updated_at = NOW()
            WHERE round_id = #{roundId}
              AND tenant_id = #{tenantId}
              AND case_id = #{caseId}
              AND state = 'PENDING'
              AND lock_version = #{lockVersion}
            """)
    int markRunning(@Param("tenantId") Long tenantId,
                    @Param("caseId") Long caseId,
                    @Param("roundId") Long roundId,
                    @Param("lockVersion") Integer lockVersion,
                    @Param("attemptId") Integer attemptId);

    /**
     * 用户主动取消正在运行的庭审轮次。
     */
    @Update("""
            UPDATE court_hearing_round
            SET state = 'CANCELLED',
                failure_reason = #{reason},
                ended_at = NOW(),
                updated_at = NOW(),
                lock_version = lock_version + 1
            WHERE round_id = #{roundId}
              AND tenant_id = #{tenantId}
              AND case_id = #{caseId}
              AND state = 'RUNNING'
            """)
    int cancelRunning(@Param("tenantId") Long tenantId,
                      @Param("caseId") Long caseId,
                      @Param("roundId") Long roundId,
                      @Param("reason") String reason);

    /**
     * 累加庭审轮次 Token 消耗。
     */
    @Update("""
            UPDATE court_hearing_round
            SET total_tokens = COALESCE(total_tokens, 0) + #{tokenUsage},
                updated_at = NOW()
            WHERE round_id = #{roundId}
              AND tenant_id = #{tenantId}
              AND case_id = #{caseId}
            """)
    int increaseTotalTokens(@Param("tenantId") Long tenantId,
                            @Param("caseId") Long caseId,
                            @Param("roundId") Long roundId,
                            @Param("tokenUsage") Integer tokenUsage);

    /**
     * 标记正在运行的庭审轮次为执行成功。
     */
    @Update("""
            UPDATE court_hearing_round
            SET state = 'SUCCEEDED',
                ended_at = NOW(),
                updated_at = NOW(),
                lock_version = lock_version + 1
            WHERE round_id = #{roundId}
              AND tenant_id = #{tenantId}
              AND case_id = #{caseId}
              AND state = 'RUNNING'
            """)
    int markSucceeded(@Param("tenantId") Long tenantId,
                      @Param("caseId") Long caseId,
                      @Param("roundId") Long roundId);

    /**
     * 标记正在运行的庭审轮次为执行失败。
     */
    @Update("""
            UPDATE court_hearing_round
            SET state = 'FAILED',
                failure_reason = #{reason},
                ended_at = NOW(),
                updated_at = NOW(),
                lock_version = lock_version + 1
            WHERE round_id = #{roundId}
              AND tenant_id = #{tenantId}
              AND case_id = #{caseId}
              AND state = 'RUNNING'
            """)
    int markFailed(@Param("tenantId") Long tenantId,
                   @Param("caseId") Long caseId,
                   @Param("roundId") Long roundId,
                   @Param("reason") String reason);
}
