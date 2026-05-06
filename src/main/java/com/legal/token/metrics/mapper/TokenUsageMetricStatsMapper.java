package com.legal.token.metrics.mapper;

import com.legal.token.metrics.dto.TokenUsageMetricDtos;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TokenUsageMetricStatsMapper {

    @Select("""
            SELECT COALESCE(SUM(GREATEST(x.token_usage, 0)), 0) AS totalTokenUsage,
                   COUNT(*) AS messageCount,
                   COUNT(DISTINCT x.user_id) AS activeUserCount
            FROM (
                SELECT s.tenant_id,
                       s.owner_user_id AS user_id,
                       m.token_usage AS token_usage,
                       m.created_at AS created_at
                FROM chat_message m
                JOIN chat_session s ON s.session_id = m.session_id
                WHERE s.tenant_id = #{tenantId}
                  AND m.role = 'ASSISTANT'
                  AND m.created_at >= #{startAt}
                  AND m.created_at <= #{endAt}
                UNION ALL
                SELECT c.tenant_id,
                       c.owner_user_id AS user_id,
                       COALESCE(m.token_input, 0) + COALESCE(m.token_output, 0) AS token_usage,
                       m.created_at AS created_at
                FROM court_hearing_message m
                JOIN court_case c ON c.case_id = m.case_id
                WHERE c.tenant_id = #{tenantId}
                  AND m.speaker_role <> 'USER'
                  AND m.created_at >= #{startAt}
                  AND m.created_at <= #{endAt}
            ) x
            """)
    TokenUsageMetricDtos.AggregatedUsage aggregateDailyUsage(@Param("tenantId") Long tenantId,
                                                            @Param("startAt") LocalDateTime startAt,
                                                            @Param("endAt") LocalDateTime endAt);

    @Select("""
            SELECT x.user_id AS userId,
                   COALESCE(u.username, CONCAT('user-', x.user_id)) AS username,
                   COALESCE(u.display_name, COALESCE(u.username, CONCAT('用户', x.user_id))) AS displayName,
                   COALESCE(SUM(GREATEST(x.token_usage, 0)), 0) AS tokenUsage,
                   COUNT(*) AS messageCount
            FROM (
                SELECT s.tenant_id,
                       s.owner_user_id AS user_id,
                       m.token_usage AS token_usage,
                       m.created_at AS created_at
                FROM chat_message m
                JOIN chat_session s ON s.session_id = m.session_id
                WHERE s.tenant_id = #{tenantId}
                  AND m.role = 'ASSISTANT'
                  AND m.created_at >= #{startAt}
                  AND m.created_at <= #{endAt}
                UNION ALL
                SELECT c.tenant_id,
                       c.owner_user_id AS user_id,
                       COALESCE(m.token_input, 0) + COALESCE(m.token_output, 0) AS token_usage,
                       m.created_at AS created_at
                FROM court_hearing_message m
                JOIN court_case c ON c.case_id = m.case_id
                WHERE c.tenant_id = #{tenantId}
                  AND m.speaker_role <> 'USER'
                  AND m.created_at >= #{startAt}
                  AND m.created_at <= #{endAt}
            ) x
            LEFT JOIN legal_user u ON u.user_id = x.user_id AND u.tenant_id = x.tenant_id
            GROUP BY x.user_id, u.username, u.display_name
            ORDER BY tokenUsage DESC, messageCount DESC, userId ASC
            LIMIT #{limit}
            """)
    List<TokenUsageMetricDtos.TopUserUsage> selectTopUsers(@Param("tenantId") Long tenantId,
                                                           @Param("startAt") LocalDateTime startAt,
                                                           @Param("endAt") LocalDateTime endAt,
                                                           @Param("limit") int limit);

    @Select("""
            SELECT DISTINCT x.tenant_id
            FROM (
                SELECT s.tenant_id
                FROM chat_message m
                JOIN chat_session s ON s.session_id = m.session_id
                WHERE m.role = 'ASSISTANT'
                  AND m.created_at >= #{startAt}
                  AND m.created_at <= #{endAt}
                UNION ALL
                SELECT c.tenant_id
                FROM court_hearing_message m
                JOIN court_case c ON c.case_id = m.case_id
                WHERE m.speaker_role <> 'USER'
                  AND m.created_at >= #{startAt}
                  AND m.created_at <= #{endAt}
            ) x
            ORDER BY x.tenant_id ASC
            LIMIT #{limit}
            """)
    List<Long> selectTenantIdsWithUsage(@Param("startAt") LocalDateTime startAt,
                                         @Param("endAt") LocalDateTime endAt,
                                         @Param("limit") int limit);
}
