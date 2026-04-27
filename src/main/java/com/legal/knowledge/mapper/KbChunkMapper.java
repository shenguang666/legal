package com.legal.knowledge.mapper;

import com.legal.knowledge.entity.KbChunkEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface KbChunkMapper {

    @Select("""
            <script>
            SELECT c.chunk_id,
                   c.tenant_id,
                   c.document_id,
                   c.doc_version,
                   c.chunk_order,
                   c.content,
                   c.content_hash,
                   c.created_at
            FROM kb_chunk c
            INNER JOIN kb_document d ON d.document_id = c.document_id
            WHERE c.tenant_id = #{tenantId}
              AND d.tenant_id = #{tenantId}
              AND d.status != 'DELETED'
              AND d.doc_version = c.doc_version
              <if test='keywords != null and keywords.size() &gt; 0'>
                AND (
                  <foreach collection='keywords' item='keyword' separator=' OR '>
                    c.content LIKE CONCAT('%', #{keyword}, '%')
                  </foreach>
                )
              </if>
            ORDER BY c.doc_version DESC, c.chunk_order ASC, c.chunk_id ASC
            LIMIT #{limit}
            </script>
            """)
    List<KbChunkEntity> searchByKeywords(@Param("tenantId") Long tenantId,
                                         @Param("keywords") List<String> keywords,
                                         @Param("limit") int limit);
}
