package com.legal.court.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.legal.court.entity.CourtCaseEvidenceEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 智能小法庭案件证据登记 Mapper。
 */
@Mapper
public interface CourtCaseEvidenceMapper extends BaseMapper<CourtCaseEvidenceEntity> {

    /**
     * 文档失效时把关联案件证据标记为失效。
     */
    @Update("""
            UPDATE court_case_evidence
            SET status = 'INVALID',
                updated_at = NOW()
            WHERE tenant_id = #{tenantId}
              AND document_id = #{documentId}
              AND status = 'ACTIVE'
            """)
    int invalidateByDocument(@Param("tenantId") Long tenantId, @Param("documentId") Long documentId);
}
