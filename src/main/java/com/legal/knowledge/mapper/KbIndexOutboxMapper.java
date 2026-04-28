package com.legal.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.legal.knowledge.entity.KbIndexOutboxEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface KbIndexOutboxMapper extends BaseMapper<KbIndexOutboxEntity> {

    @Select("""
            SELECT id,
                   tenant_id,
                   document_id,
                   doc_version,
                   op,
                   status,
                   retry_count,
                   next_retry_at,
                   created_at,
                   updated_at
            FROM kb_index_outbox
            WHERE (status = 'PENDING' OR status = 'FAILED')
              AND retry_count < #{maxRetries}
              AND (next_retry_at IS NULL OR next_retry_at <= NOW())
            ORDER BY id ASC
            LIMIT #{limit}
            """)
    List<KbIndexOutboxEntity> selectReadyTasks(@Param("limit") int limit,
                                               @Param("maxRetries") int maxRetries);

    @Update("""
            UPDATE kb_index_outbox
            SET status = 'PROCESSING',
                updated_at = NOW()
            WHERE id = #{id}
              AND (status = 'PENDING' OR status = 'FAILED')
            """)
    int markProcessing(@Param("id") Long id);

    @Update("""
            UPDATE kb_index_outbox
            SET status = 'DONE',
                updated_at = NOW()
            WHERE id = #{id}
            """)
    int markDone(@Param("id") Long id);

    @Update("""
            UPDATE kb_index_outbox
            SET status = 'FAILED',
                retry_count = retry_count + 1,
                next_retry_at = DATE_ADD(NOW(), INTERVAL #{retryDelaySeconds} SECOND),
                updated_at = NOW()
            WHERE id = #{id}
            """)
    int markFailed(@Param("id") Long id,
                   @Param("retryDelaySeconds") long retryDelaySeconds);
}
