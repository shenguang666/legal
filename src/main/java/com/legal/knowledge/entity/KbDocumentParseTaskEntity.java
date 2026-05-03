package com.legal.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.legal.enums.DocumentParseMethod;
import com.legal.enums.DocumentParseStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档解析任务实体。
 */
@Data
@TableName("kb_document_parse_task")
public class KbDocumentParseTaskEntity {

    /** 文档解析任务主键ID。 */
    @TableId(value = "task_id", type = IdType.AUTO)
    private Long taskId;
    /** 租户ID。 */
    private Long tenantId;
    /** 文档ID。 */
    private Long documentId;
    /** 文档版本号。 */
    private Integer docVersion;
    /** 文档解析方式（NATIVE/MINERU_PRECISE）。 */
    private DocumentParseMethod parseMethod;
    /** 解析任务状态（PENDING/PROCESSING/COMPLETED/FAILED）。 */
    private DocumentParseStatus parseStatus;
    /** 是否在该解析任务中启用内容清洗。 */
    private Boolean cleaningEnabled;
    /** 上传文件名。 */
    private String fileName;
    /** 待解析文件内容，仅用于异步提交 MinerU。 */
    private byte[] fileContent;
    /** MinerU 批次ID。 */
    private String mineruBatchId;
    /** MinerU 文件数据ID。 */
    private String mineruDataId;
    /** MinerU 完整解析结果压缩包地址。 */
    private String mineruFullZipUrl;
    /** 解析任务失败后的重试次数。 */
    private Integer retryCount;
    /** 解析任务失败原因。 */
    private String errorMessage;
    /** 解析任务开始时间。 */
    private LocalDateTime startedAt;
    /** 解析任务完成时间。 */
    private LocalDateTime completedAt;
    /** 下次允许重试时间。 */
    private LocalDateTime nextRetryAt;
    /** 创建时间。 */
    private LocalDateTime createdAt;
    /** 更新时间。 */
    private LocalDateTime updatedAt;
}
