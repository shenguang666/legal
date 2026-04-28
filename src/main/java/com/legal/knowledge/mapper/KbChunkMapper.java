package com.legal.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.legal.knowledge.entity.KbChunkEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface KbChunkMapper extends BaseMapper<KbChunkEntity> {

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

    @Select("""
            SELECT chunk_id,
                   tenant_id,
                   document_id,
                   doc_version,
                   chunk_order,
                   content,
                   content_hash,
                   created_at
            FROM kb_chunk
            WHERE tenant_id = #{tenantId}
              AND document_id = #{documentId}
              AND doc_version = #{docVersion}
            ORDER BY chunk_order ASC, chunk_id ASC
            """)
    List<KbChunkEntity> selectByDocVersion(@Param("tenantId") Long tenantId,
                                           @Param("documentId") Long documentId,
                                           @Param("docVersion") int docVersion);

    @Delete("""
            DELETE FROM kb_chunk
            WHERE tenant_id = #{tenantId}
              AND document_id = #{documentId}
              AND doc_version = #{docVersion}
            """)
    int deleteByDocVersion(@Param("tenantId") Long tenantId,
                           @Param("documentId") Long documentId,
                           @Param("docVersion") int docVersion);

    @Insert("""
            INSERT INTO kb_chunk (tenant_id, document_id, doc_version, chunk_order, content, content_hash, created_at)
            SELECT tenant_id,
                   document_id,
                   #{toVersion},
                   chunk_order,
                   content,
                   content_hash,
                   NOW()
            FROM kb_chunk
            WHERE tenant_id = #{tenantId}
              AND document_id = #{documentId}
              AND doc_version = #{fromVersion}
            """)
    int copyVersion(@Param("tenantId") Long tenantId,
                    @Param("documentId") Long documentId,
                    @Param("fromVersion") int fromVersion,
                    @Param("toVersion") int toVersion);
}
