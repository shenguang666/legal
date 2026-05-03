package com.legal.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档内容清洗日志实体。
 */
@Data
@TableName("kb_document_cleaning_log")
public class KbDocumentCleaningLogEntity {

    /** 文档清洗日志主键ID。 */
    @TableId(value = "cleaning_log_id", type = IdType.AUTO)
    private Long cleaningLogId;
    /** 租户ID。 */
    private Long tenantId;
    /** 文档ID。 */
    private Long documentId;
    /** 文档版本号。 */
    private Integer docVersion;
    /** 文档解析方式（NATIVE/MINERU_PRECISE）。 */
    private String parseMethod;
    /** 清洗内容格式（TEXT/MARKDOWN）。 */
    private String contentFormat;
    /** 清洗前字符数。 */
    private Integer originalChars;
    /** 清洗后字符数。 */
    private Integer cleanedChars;
    /** 被清洗删除的行数。 */
    private Integer removedLineCount;
    /** 被过滤删除的低质量切片数量。 */
    private Integer removedChunkCount;
    /** 清洗原因统计JSON。 */
    private String reasonSummaryJson;
    /** 被清洗内容样例JSON，按配置限制数量和长度。 */
    private String removedSamplesJson;
    /** 创建时间。 */
    private LocalDateTime createdAt;
}
