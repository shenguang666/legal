package com.legal.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.legal.enums.KbDocumentStatus;
import com.legal.enums.KbIndexStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库文档表实体。
 */
@Data
@TableName("kb_document")
public class KbDocumentEntity {

    /** 文档主键ID。 */
    @TableId(value = "document_id", type = IdType.AUTO)
    private Long documentId;
    /** 租户ID。 */
    private Long tenantId;
    /** 所属用户ID。 */
    private Long ownerUserId;
    /** 文档标题。 */
    private String title;
    /** 文档来源。 */
    private String source;
    /** 文档状态（PENDING/PROCESSING/DELETED）。 */
	private KbDocumentStatus status;
    /** 文档版本号。 */
    private Integer docVersion;
    /** 索引状态（PENDING/PROCESSING/COMPLETED）。 */
	private KbIndexStatus indexStatus;
    /** 创建时间。 */
    private LocalDateTime createdAt;
    /** 更新时间。 */
    private LocalDateTime updatedAt;
}
