package com.legal.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.legal.enums.KbChunkType;
import com.legal.knowledge.entity.KbChunkEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mapper
public interface KbChunkMapper extends BaseMapper<KbChunkEntity> {

    @Select("""
            <script>
            SELECT c.chunk_id,
                   c.tenant_id,
                   c.document_id,
                   c.doc_version,
                   c.chunk_order,
                   c.chunk_type,
                   c.parent_chunk_id,
                   c.content,
                   c.content_hash,
                   c.created_at
            FROM kb_chunk c
            INNER JOIN kb_document d ON d.document_id = c.document_id
            WHERE c.tenant_id = #{tenantId}
              AND d.tenant_id = #{tenantId}
              AND d.status != 'DELETED'
              AND d.doc_version = c.doc_version
              AND (c.chunk_type IS NULL OR c.chunk_type != 'PARENT')
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
                   chunk_type,
                   parent_chunk_id,
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

    @Select("""
            <script>
            SELECT chunk_id,
                   tenant_id,
                   document_id,
                   doc_version,
                   chunk_order,
                   chunk_type,
                   parent_chunk_id,
                   content,
                   content_hash,
                   created_at
            FROM kb_chunk
            WHERE tenant_id = #{tenantId}
              AND chunk_type = 'PARENT'
              AND chunk_id IN
              <foreach collection='chunkIds' item='chunkId' open='(' separator=',' close=')'>
                #{chunkId}
              </foreach>
            </script>
            """)
    List<KbChunkEntity> selectParentChunksByIds(@Param("tenantId") Long tenantId,
                                                @Param("chunkIds") List<Long> chunkIds);

    @Select("""
            <script>
            SELECT c.chunk_id,
                   c.tenant_id,
                   c.document_id,
                   c.doc_version,
                   c.chunk_order,
                   c.chunk_type,
                   c.parent_chunk_id,
                   c.content,
                   c.content_hash,
                   c.created_at
            FROM kb_chunk c
            INNER JOIN kb_document d ON d.document_id = c.document_id
            WHERE c.tenant_id = #{tenantId}
              AND d.tenant_id = #{tenantId}
              AND d.status != 'DELETED'
              AND d.doc_version = c.doc_version
              AND (c.chunk_type IS NULL OR c.chunk_type != 'PARENT')
              AND c.document_id IN
              <foreach collection='documentIds' item='documentId' open='(' separator=',' close=')'>
                #{documentId}
              </foreach>
            ORDER BY c.document_id ASC, c.chunk_order ASC, c.chunk_id ASC
            </script>
            """)
    List<KbChunkEntity> selectCourtAllowedChunksByDocuments(@Param("tenantId") Long tenantId,
                                                            @Param("documentIds") List<Long> documentIds);

    @Delete("""
            DELETE FROM kb_chunk
            WHERE tenant_id = #{tenantId}
              AND document_id = #{documentId}
              AND doc_version = #{docVersion}
            """)
    int deleteByDocVersion(@Param("tenantId") Long tenantId,
                           @Param("documentId") Long documentId,
                           @Param("docVersion") int docVersion);

    @Delete("""
            DELETE FROM kb_chunk
            WHERE tenant_id = #{tenantId}
              AND document_id = #{documentId}
            """)
    int deleteByDocument(@Param("tenantId") Long tenantId,
                         @Param("documentId") Long documentId);

    default int copyVersion(@Param("tenantId") Long tenantId,
                            @Param("documentId") Long documentId,
                            @Param("fromVersion") int fromVersion,
                            @Param("toVersion") int toVersion) {
        List<KbChunkEntity> sourceChunks = selectByDocVersion(tenantId, documentId, fromVersion);
        Map<Long, Long> copiedParentIds = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        int copied = 0;
        for (KbChunkEntity source : sourceChunks) {
            KbChunkType chunkType = source.getChunkType() == null ? KbChunkType.NORMAL : source.getChunkType();
            if (chunkType == KbChunkType.CHILD) {
                continue;
            }
            KbChunkEntity target = copyBase(source, toVersion, chunkType, null, now);
            insert(target);
            if (chunkType == KbChunkType.PARENT) {
                copiedParentIds.put(source.getChunkId(), target.getChunkId());
            }
            copied++;
        }
        for (KbChunkEntity source : sourceChunks) {
            KbChunkType chunkType = source.getChunkType() == null ? KbChunkType.NORMAL : source.getChunkType();
            if (chunkType != KbChunkType.CHILD) {
                continue;
            }
            Long copiedParentId = copiedParentIds.get(source.getParentChunkId());
            if (copiedParentId == null) {
                continue;
            }
            KbChunkEntity target = copyBase(source, toVersion, KbChunkType.CHILD, copiedParentId, now);
            insert(target);
            copied++;
        }
        return copied;
    }

    private KbChunkEntity copyBase(KbChunkEntity source, int toVersion, KbChunkType chunkType, Long parentChunkId, LocalDateTime now) {
        KbChunkEntity target = new KbChunkEntity();
        target.setTenantId(source.getTenantId());
        target.setDocumentId(source.getDocumentId());
        target.setDocVersion(toVersion);
        target.setChunkOrder(source.getChunkOrder());
        target.setChunkType(chunkType);
        target.setParentChunkId(parentChunkId);
        target.setContent(source.getContent());
        target.setContentHash(source.getContentHash());
        target.setCreatedAt(now);
        return target;
    }
}
