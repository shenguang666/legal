package com.legal.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.legal.enums.DocumentParseMethod;
import com.legal.enums.DocumentParseStatus;
import com.legal.enums.KbDocumentBizType;
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
    /** 文档业务类型（知识库/风险规则/天眼审查）。 */
    private KbDocumentBizType bizType;
    /** 文档状态（PENDING/PROCESSING/DELETED）。 */
	private KbDocumentStatus status;
    /** 文档版本号。 */
    private Integer docVersion;
    /** 索引状态（PENDING/PROCESSING/COMPLETED）。 */
	private KbIndexStatus indexStatus;
    /** 文档解析方式（NATIVE/MINERU_PRECISE）。 */
    private DocumentParseMethod parseMethod;
    /** 文档解析状态（PENDING/PROCESSING/COMPLETED/FAILED）。 */
    private DocumentParseStatus parseStatus;
    /** 文档解析失败原因，用于前端展示和排查。 */
    private String parseFailureReason;
    /** MinerU 批次ID，用于关联外部精准解析任务。 */
    private String mineruBatchId;
    /** MinerU 文件数据ID，用于关联批次内单个文件。 */
    private String mineruDataId;
    /** MinerU 解析结果压缩包地址，仅用于后端下载解析产物。 */
    private String mineruFullZipUrl;
    /** 最近一次解析开始时间。 */
    private LocalDateTime parseStartedAt;
    /** 最近一次解析完成时间。 */
    private LocalDateTime parseCompletedAt;
    /** 创建时间。 */
    private LocalDateTime createdAt;
    /** 更新时间。 */
    private LocalDateTime updatedAt;
}
