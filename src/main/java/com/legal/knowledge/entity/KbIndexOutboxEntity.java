package com.legal.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 索引任务出站表实体。
 */
@Data
@TableName("kb_index_outbox")
public class KbIndexOutboxEntity {

    /** 出站事件主键ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 租户ID。 */
    private Long tenantId;
    /** 文档ID。 */
    private Long documentId;
    /** 文档版本号。 */
    private Integer docVersion;
    /** 索引操作类型（UPSERT/DELETE）。 */
    private String op;
    /** 出站任务状态（PENDING/PROCESSING/DONE/FAILED）。 */
    private String status;
    /** 重试次数。 */
    private Integer retryCount;
    /** 下次重试时间。 */
    private LocalDateTime nextRetryAt;
    /** 创建时间。 */
    private LocalDateTime createdAt;
    /** 更新时间。 */
    private LocalDateTime updatedAt;
}
