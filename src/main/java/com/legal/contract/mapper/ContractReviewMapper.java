package com.legal.contract.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.legal.contract.entity.ContractReviewEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ContractReviewMapper extends BaseMapper<ContractReviewEntity> {

    @Select("""
            SELECT review_id,
                   tenant_id,
                   document_id,
                   doc_version,
                   owner_user_id,
                   triggered_by_user_id,
                   status,
                   risk_level,
                   risk_count,
                   hit_rule_count,
                   total_field_count,
                   extracted_field_count,
                   missing_field_count,
                   summary_text,
                   failure_reason,
                   started_at,
                   completed_at,
                   created_at,
                   updated_at
            FROM contract_review
            WHERE tenant_id = #{tenantId}
              AND document_id = #{documentId}
            ORDER BY review_id DESC
            LIMIT 1
            """)
    ContractReviewEntity selectLatestByDocument(@Param("tenantId") Long tenantId,
                                                @Param("documentId") Long documentId);
}
