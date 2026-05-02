package com.legal.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.legal.knowledge.entity.KbDocumentParseTaskEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface KbDocumentParseTaskMapper extends BaseMapper<KbDocumentParseTaskEntity> {

    @Select("""
            SELECT task_id,
                   tenant_id,
                   document_id,
                   doc_version,
                   parse_method,
                   parse_status,
                   file_name,
                   file_content,
                   mineru_batch_id,
                   mineru_data_id,
                   mineru_full_zip_url,
                   retry_count,
                   error_message,
                   started_at,
                   completed_at,
                   next_retry_at,
                   created_at,
                   updated_at
            FROM kb_document_parse_task
            WHERE parse_status IN ('PENDING', 'FAILED')
              AND retry_count < #{maxRetries}
              AND (next_retry_at IS NULL OR next_retry_at <= NOW())
            ORDER BY created_at ASC, task_id ASC
            LIMIT #{limit}
            """)
    List<KbDocumentParseTaskEntity> selectReadyTasks(@Param("limit") int limit,
                                                     @Param("maxRetries") int maxRetries);

    @Update("""
            UPDATE kb_document_parse_task
            SET parse_status = 'PROCESSING',
                started_at = NOW(),
                updated_at = NOW()
            WHERE task_id = #{taskId}
              AND parse_status IN ('PENDING', 'FAILED')
            """)
    int markProcessing(@Param("taskId") Long taskId);

    @Update("""
            UPDATE kb_document_parse_task
            SET parse_status = 'COMPLETED',
                error_message = NULL,
                completed_at = NOW(),
                updated_at = NOW()
            WHERE task_id = #{taskId}
            """)
    int markCompleted(@Param("taskId") Long taskId);

    @Update("""
            UPDATE kb_document_parse_task
            SET parse_status = 'FAILED',
                retry_count = retry_count + 1,
                error_message = #{errorMessage},
                next_retry_at = DATE_ADD(NOW(), INTERVAL #{retryDelaySeconds} SECOND),
                updated_at = NOW()
            WHERE task_id = #{taskId}
            """)
    int markFailed(@Param("taskId") Long taskId,
                   @Param("errorMessage") String errorMessage,
                   @Param("retryDelaySeconds") long retryDelaySeconds);

    @Update("""
            UPDATE kb_document_parse_task
            SET file_content = NULL,
                updated_at = NOW()
            WHERE task_id = #{taskId}
            """)
    int clearFileContent(@Param("taskId") Long taskId);
}
