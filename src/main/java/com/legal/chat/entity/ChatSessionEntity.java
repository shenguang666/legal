package com.legal.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 问答会话表实体。
 */
@Data
@TableName("chat_session")
public class ChatSessionEntity {

    /** 会话ID。 */
    @TableId(value = "session_id", type = IdType.INPUT)
    private String sessionId;
    /** 租户ID。 */
    private Long tenantId;
    /** 所属用户ID。 */
    private Long ownerUserId;
    /** 会话标题。 */
    private String title;
    /** 创建时间。 */
    private LocalDateTime createdAt;
    /** 最后活跃时间。 */
    private LocalDateTime lastActiveAt;
}
