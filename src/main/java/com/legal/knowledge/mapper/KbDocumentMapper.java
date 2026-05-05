package com.legal.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.legal.enums.DocumentParseStatus;
import com.legal.enums.KbDocumentStatus;
import com.legal.knowledge.entity.KbDocumentEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface KbDocumentMapper extends BaseMapper<KbDocumentEntity> {

    @Update("""
            UPDATE kb_document
            SET parse_status = #{parseStatus},
                parse_failure_reason = NULL,
                status = #{status},
                updated_at = NOW()
            WHERE tenant_id = #{tenantId}
              AND document_id = #{documentId}
            """)
    int resetParseFailure(@Param("tenantId") Long tenantId,
                          @Param("documentId") Long documentId,
                          @Param("parseStatus") DocumentParseStatus parseStatus,
                          @Param("status") KbDocumentStatus status);

    @Update("""
            UPDATE kb_document
            SET parse_failure_reason = NULL,
                updated_at = NOW()
            WHERE tenant_id = #{tenantId}
              AND document_id = #{documentId}
              AND doc_version = #{docVersion}
            """)
    int clearParseFailureReason(@Param("tenantId") Long tenantId,
                                @Param("documentId") Long documentId,
                                @Param("docVersion") Integer docVersion);
}
