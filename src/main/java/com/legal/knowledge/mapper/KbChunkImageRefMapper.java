package com.legal.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.legal.knowledge.entity.KbChunkImageRefEntity;
import com.legal.knowledge.entity.KbDocumentImageAssetEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface KbChunkImageRefMapper extends BaseMapper<KbChunkImageRefEntity> {

    @Select("""
            <script>
            SELECT a.image_asset_id,
                   a.tenant_id,
                   a.document_id,
                   a.doc_version,
                   a.biz_type,
                   a.owner_user_id,
                   a.username,
                   a.original_path,
                   a.oss_bucket,
                   a.oss_object_key,
                   a.public_url,
                   a.mime_type,
                   a.file_ext,
                   a.size_bytes,
                   a.content_hash,
                   a.description,
                   a.caption_status,
                   a.caption_error,
                   a.created_at,
                   a.updated_at
            FROM kb_chunk_image_ref r
            INNER JOIN kb_document_image_asset a ON a.image_asset_id = r.image_asset_id
            WHERE r.tenant_id = #{tenantId}
              AND r.chunk_id IN
              <foreach collection='chunkIds' item='chunkId' open='(' separator=',' close=')'>
                #{chunkId}
              </foreach>
            ORDER BY r.chunk_id ASC, r.image_order ASC, r.chunk_image_ref_id ASC
            </script>
            """)
    List<KbDocumentImageAssetEntity> selectAssetsByChunkIds(@Param("tenantId") Long tenantId,
                                                            @Param("chunkIds") List<Long> chunkIds);

    @Select("""
            <script>
            SELECT r.chunk_id,
                   r.image_order,
                   a.image_asset_id,
                   a.tenant_id,
                   a.document_id,
                   a.doc_version,
                   a.biz_type,
                   a.owner_user_id,
                   a.username,
                   a.original_path,
                   a.oss_bucket,
                   a.oss_object_key,
                   a.public_url,
                   a.mime_type,
                   a.file_ext,
                   a.size_bytes,
                   a.content_hash,
                   a.description,
                   a.caption_status,
                   a.caption_error,
                   a.created_at,
                   a.updated_at
            FROM kb_chunk_image_ref r
            INNER JOIN kb_document_image_asset a ON a.image_asset_id = r.image_asset_id
            WHERE r.tenant_id = #{tenantId}
              AND r.chunk_id IN
              <foreach collection='chunkIds' item='chunkId' open='(' separator=',' close=')'>
                #{chunkId}
              </foreach>
            ORDER BY r.chunk_id ASC, r.image_order ASC, r.chunk_image_ref_id ASC
            </script>
            """)
    List<ChunkImageAssetRow> selectImageRowsByChunkIds(@Param("tenantId") Long tenantId,
                                                       @Param("chunkIds") List<Long> chunkIds);

    @Delete("""
            DELETE r FROM kb_chunk_image_ref r
            INNER JOIN kb_chunk c ON c.chunk_id = r.chunk_id
            WHERE r.tenant_id = #{tenantId}
              AND c.document_id = #{documentId}
              AND c.doc_version = #{docVersion}
            """)
    int deleteByDocumentVersion(@Param("tenantId") Long tenantId,
                                @Param("documentId") Long documentId,
                                @Param("docVersion") int docVersion);

    @Delete("""
            DELETE r FROM kb_chunk_image_ref r
            INNER JOIN kb_chunk c ON c.chunk_id = r.chunk_id
            WHERE r.tenant_id = #{tenantId}
              AND c.document_id = #{documentId}
            """)
    int deleteByDocument(@Param("tenantId") Long tenantId,
                         @Param("documentId") Long documentId);

    class ChunkImageAssetRow extends KbDocumentImageAssetEntity {

        /** 切片ID。 */
        private Long chunkId;
        /** 图片在切片中的出现顺序。 */
        private Integer imageOrder;

        public Long getChunkId() {
            return chunkId;
        }

        public void setChunkId(Long chunkId) {
            this.chunkId = chunkId;
        }

        public Integer getImageOrder() {
            return imageOrder;
        }

        public void setImageOrder(Integer imageOrder) {
            this.imageOrder = imageOrder;
        }
    }
}
