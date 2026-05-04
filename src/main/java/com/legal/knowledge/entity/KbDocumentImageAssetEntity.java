package com.legal.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.legal.enums.ImageCaptionStatus;
import com.legal.enums.KbDocumentBizType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MinerU 文档图片资产实体。
 */
@Data
@TableName("kb_document_image_asset")
public class KbDocumentImageAssetEntity {

    /** 图片资产主键ID。 */
    @TableId(value = "image_asset_id", type = IdType.AUTO)
    private Long imageAssetId;
    /** 租户ID。 */
    private Long tenantId;
    /** 文档ID。 */
    private Long documentId;
    /** 文档版本号。 */
    private Integer docVersion;
    /** 文档业务类型（KNOWLEDGE/RISK_RULE/TIANYAN_REVIEW）。 */
    private KbDocumentBizType bizType;
    /** 所属用户ID。 */
    private Long ownerUserId;
    /** 上传用户名快照。 */
    private String username;
    /** MinerU 结果包内图片原始相对路径。 */
    private String originalPath;
    /** OSS Bucket 名称。 */
    private String ossBucket;
    /** OSS 对象 Key。 */
    private String ossObjectKey;
    /** 图片公开访问地址或后端生成的可访问地址。 */
    private String publicUrl;
    /** 图片 MIME 类型。 */
    private String mimeType;
    /** 图片文件扩展名。 */
    private String fileExt;
    /** 图片文件大小字节数。 */
    private Long sizeBytes;
    /** 图片内容 SHA-256 哈希。 */
    private String contentHash;
    /** 图生文模型生成的图片描述。 */
    private String description;
    /** 图片描述状态（PENDING/SUCCESS/FAILED/SKIPPED）。 */
    private ImageCaptionStatus captionStatus;
    /** 图片描述失败或跳过原因。 */
    private String captionError;
    /** 创建时间。 */
    private LocalDateTime createdAt;
    /** 更新时间。 */
    private LocalDateTime updatedAt;
}
