package com.legal.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.legal.knowledge.entity.KbDocumentImageAssetEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface KbDocumentImageAssetMapper extends BaseMapper<KbDocumentImageAssetEntity> {

    @Select("""
            SELECT image_asset_id,
                   tenant_id,
                   document_id,
                   doc_version,
                   biz_type,
                   owner_user_id,
                   username,
                   original_path,
                   oss_bucket,
                   oss_object_key,
                   public_url,
                   mime_type,
                   file_ext,
                   size_bytes,
                   content_hash,
                   description,
                   caption_status,
                   caption_error,
                   created_at,
                   updated_at
            FROM kb_document_image_asset
            WHERE tenant_id = #{tenantId}
              AND document_id = #{documentId}
              AND doc_version = #{docVersion}
            ORDER BY image_asset_id ASC
            """)
    List<KbDocumentImageAssetEntity> selectByDocVersion(@Param("tenantId") Long tenantId,
                                                        @Param("documentId") Long documentId,
                                                        @Param("docVersion") int docVersion);

    @Select("""
            SELECT image_asset_id,
                   tenant_id,
                   document_id,
                   doc_version,
                   biz_type,
                   owner_user_id,
                   username,
                   original_path,
                   oss_bucket,
                   oss_object_key,
                   public_url,
                   mime_type,
                   file_ext,
                   size_bytes,
                   content_hash,
                   description,
                   caption_status,
                   caption_error,
                   created_at,
                   updated_at
            FROM kb_document_image_asset
            WHERE tenant_id = #{tenantId}
              AND document_id = #{documentId}
            ORDER BY doc_version ASC, image_asset_id ASC
            """)
    List<KbDocumentImageAssetEntity> selectByDocument(@Param("tenantId") Long tenantId,
                                                      @Param("documentId") Long documentId);

    @Delete("""
            DELETE FROM kb_document_image_asset
            WHERE tenant_id = #{tenantId}
              AND document_id = #{documentId}
              AND doc_version = #{docVersion}
            """)
    int deleteByDocVersion(@Param("tenantId") Long tenantId,
                           @Param("documentId") Long documentId,
                           @Param("docVersion") int docVersion);

    @Delete("""
            DELETE FROM kb_document_image_asset
            WHERE tenant_id = #{tenantId}
              AND document_id = #{documentId}
            """)
    int deleteByDocument(@Param("tenantId") Long tenantId,
                         @Param("documentId") Long documentId);
}
