package com.legal.memory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话摘要记忆表实体。
 */
@Data
@TableName("chat_memory_summary")
public class ChatMemorySummaryEntity {

    /** 摘要主键ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 租户ID。 */
    private Long tenantId;
    /** 用户ID。 */
    private Long userId;
    /** 会话ID。 */
    private String sessionId;
    /** 摘要文本（建议JSON）。 */
    private String summaryText;
    /** 已摘要到第几轮（assistant 消息落库次数）。 */
    private Integer roundCount;
    /** 摘要版本。 */
    private Integer version;
    /** 创建时间。 */
    private LocalDateTime createdAt;
    /** 更新时间。 */
    private LocalDateTime updatedAt;
}
