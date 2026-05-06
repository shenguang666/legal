package com.legal.court.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.legal.court.entity.CourtGraphEventEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 智能小法庭图谱事件 Mapper。
 */
@Mapper
public interface CourtGraphEventMapper extends BaseMapper<CourtGraphEventEntity> {

    /**
     * 查询等待投影且已到重试时间的图谱事件。
     */
    @Select("""
            SELECT event_id,
                   tenant_id,
                   case_id,
                   round_id,
                   event_type,
                   payload_json,
                   status,
                   retry_count,
                   error_message,
                   next_retry_at,
                   applied_at,
                   created_at,
                   updated_at
            FROM court_graph_event
            WHERE status = 'PENDING'
              AND (next_retry_at IS NULL OR next_retry_at <= NOW())
            ORDER BY event_id ASC
            LIMIT #{limit}
            """)
    List<CourtGraphEventEntity> selectReadyPending(@Param("limit") int limit);

    /**
     * 标记图谱事件已成功投影。
     */
    @Update("""
            UPDATE court_graph_event
            SET status = 'APPLIED',
                error_message = NULL,
                applied_at = NOW(),
                updated_at = NOW()
            WHERE event_id = #{eventId}
              AND status = 'PENDING'
            """)
    int markApplied(@Param("eventId") Long eventId);

    /**
     * 标记图谱事件等待下次重试。
     */
    @Update("""
            UPDATE court_graph_event
            SET retry_count = #{retryCount},
                error_message = #{errorMessage},
                next_retry_at = DATE_ADD(NOW(), INTERVAL #{delaySeconds} SECOND),
                updated_at = NOW()
            WHERE event_id = #{eventId}
              AND status = 'PENDING'
            """)
    int markRetry(@Param("eventId") Long eventId,
                  @Param("retryCount") int retryCount,
                  @Param("errorMessage") String errorMessage,
                  @Param("delaySeconds") long delaySeconds);

    /**
     * 标记图谱事件进入死信状态。
     */
    @Update("""
            UPDATE court_graph_event
            SET status = 'DEAD',
                retry_count = #{retryCount},
                error_message = #{errorMessage},
                updated_at = NOW()
            WHERE event_id = #{eventId}
            """)
    int markDead(@Param("eventId") Long eventId,
                 @Param("retryCount") int retryCount,
                 @Param("errorMessage") String errorMessage);

    /**
     * 统计案件待投影事件数量。
     */
    @Select("""
            SELECT COUNT(*)
            FROM court_graph_event
            WHERE tenant_id = #{tenantId}
              AND case_id = #{caseId}
              AND status = 'PENDING'
            """)
    long countPendingByCase(@Param("tenantId") Long tenantId, @Param("caseId") Long caseId);

    /**
     * 按事件ID分页查询案件图谱事件，用于全量重建回放。
     */
    @Select("""
            SELECT event_id,
                   tenant_id,
                   case_id,
                   round_id,
                   event_type,
                   payload_json,
                   status,
                   retry_count,
                   error_message,
                   next_retry_at,
                   applied_at,
                   created_at,
                   updated_at
            FROM court_graph_event
            WHERE tenant_id = #{tenantId}
              AND case_id = #{caseId}
              AND event_id > #{afterEventId}
              AND event_type <> 'DELETE_CASE_GRAPH'
            ORDER BY event_id ASC
            LIMIT #{limit}
            """)
    List<CourtGraphEventEntity> selectCaseEventsForReplay(@Param("tenantId") Long tenantId,
                                                          @Param("caseId") Long caseId,
                                                          @Param("afterEventId") Long afterEventId,
                                                          @Param("limit") int limit);

    /**
     * 重建回放后强制标记事件为已投影。
     */
    @Update("""
            UPDATE court_graph_event
            SET status = 'APPLIED',
                error_message = NULL,
                applied_at = NOW(),
                updated_at = NOW()
            WHERE event_id = #{eventId}
            """)
    int markAppliedAfterReplay(@Param("eventId") Long eventId);
}
