package com.legal.memory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.legal.enums.MemoryTaskStatus;
import com.legal.enums.MemoryTaskType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 记忆异步任务 outbox。
 */
@Data
@TableName("memory_task_outbox")
public class MemoryTaskOutboxEntity {

    /** 任务主键ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 租户ID。 */
    private Long tenantId;
    /** 用户ID。 */
    private Long userId;
    /** 会话ID。 */
    private String sessionId;
    /** 任务类型。 */
    private MemoryTaskType taskType;
    /** 任务参数 JSON。 */
    private String payload;
    /** 任务状态。 */
    private MemoryTaskStatus status;
    /** 重试次数。 */
    private Integer retryCount;
    /** 下次重试时间。 */
    private LocalDateTime nextRetryAt;
    /** 最近失败原因。 */
    private String lastError;
    /** 创建时间。 */
    private LocalDateTime createdAt;
    /** 更新时间。 */
    private LocalDateTime updatedAt;
}
