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
            SELECT COALESCE(SUM(GREATEST(m.token_usage, 0)), 0) AS totalTokenUsage,
                   COUNT(m.message_id) AS messageCount,
                   COUNT(DISTINCT s.owner_user_id) AS activeUserCount
            FROM chat_message m
            JOIN chat_session s ON s.session_id = m.session_id
            WHERE s.tenant_id = #{tenantId}
              AND m.role = 'ASSISTANT'
              AND m.created_at >= #{startAt}
              AND m.created_at <= #{endAt}
            """)
    TokenUsageMetricDtos.AggregatedUsage aggregateDailyUsage(@Param("tenantId") Long tenantId,
                                                            @Param("startAt") LocalDateTime startAt,
                                                            @Param("endAt") LocalDateTime endAt);

    @Select("""
            SELECT s.owner_user_id AS userId,
                   COALESCE(u.username, CONCAT('user-', s.owner_user_id)) AS username,
                   COALESCE(u.display_name, COALESCE(u.username, CONCAT('用户', s.owner_user_id))) AS displayName,
                   COALESCE(SUM(GREATEST(m.token_usage, 0)), 0) AS tokenUsage,
                   COUNT(m.message_id) AS messageCount
            FROM chat_message m
            JOIN chat_session s ON s.session_id = m.session_id
            LEFT JOIN legal_user u ON u.user_id = s.owner_user_id AND u.tenant_id = s.tenant_id
            WHERE s.tenant_id = #{tenantId}
              AND m.role = 'ASSISTANT'
              AND m.created_at >= #{startAt}
              AND m.created_at <= #{endAt}
            GROUP BY s.owner_user_id, u.username, u.display_name
            ORDER BY tokenUsage DESC, messageCount DESC, userId ASC
            LIMIT #{limit}
            """)
    List<TokenUsageMetricDtos.TopUserUsage> selectTopUsers(@Param("tenantId") Long tenantId,
                                                           @Param("startAt") LocalDateTime startAt,
                                                           @Param("endAt") LocalDateTime endAt,
                                                           @Param("limit") int limit);

    @Select("""
            SELECT DISTINCT s.tenant_id
            FROM chat_message m
            JOIN chat_session s ON s.session_id = m.session_id
            WHERE m.role = 'ASSISTANT'
              AND m.created_at >= #{startAt}
              AND m.created_at <= #{endAt}
            ORDER BY s.tenant_id ASC
            LIMIT #{limit}
            """)
    List<Long> selectTenantIdsWithUsage(@Param("startAt") LocalDateTime startAt,
                                         @Param("endAt") LocalDateTime endAt,
                                         @Param("limit") int limit);
}
