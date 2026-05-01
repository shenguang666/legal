package com.legal.contract.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.legal.contract.entity.ContractReviewTaskEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ContractReviewTaskMapper extends BaseMapper<ContractReviewTaskEntity> {

    @Select("""
            SELECT id,
                   tenant_id,
                   review_id,
                   document_id,
                   doc_version,
                   status,
                   retry_count,
                   next_retry_at,
                   last_error,
                   created_at,
                   updated_at
            FROM contract_review_task
            WHERE (status = 'PENDING' OR status = 'FAILED')
              AND retry_count < #{maxRetries}
              AND (next_retry_at IS NULL OR next_retry_at <= NOW())
            ORDER BY id ASC
            LIMIT #{limit}
            """)
    List<ContractReviewTaskEntity> selectReadyTasks(@Param("limit") int limit,
                                                    @Param("maxRetries") int maxRetries);

    @Update("""
            UPDATE contract_review_task
            SET status = 'PROCESSING',
                updated_at = NOW()
            WHERE id = #{id}
              AND (status = 'PENDING' OR status = 'FAILED')
            """)
    int markProcessing(@Param("id") Long id);

    @Update("""
            UPDATE contract_review_task
            SET status = 'DONE',
                updated_at = NOW()
            WHERE id = #{id}
            """)
    int markDone(@Param("id") Long id);

    @Update("""
            UPDATE contract_review_task
            SET status = 'FAILED',
                retry_count = retry_count + 1,
                next_retry_at = DATE_ADD(NOW(), INTERVAL #{retryDelaySeconds} SECOND),
                last_error = LEFT(#{lastError}, 1000),
                updated_at = NOW()
            WHERE id = #{id}
            """)
    int markFailed(@Param("id") Long id,
                   @Param("retryDelaySeconds") long retryDelaySeconds,
                   @Param("lastError") String lastError);
}
