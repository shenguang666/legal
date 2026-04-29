package com.legal.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.legal.enums.ChatMessageRole;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 问答消息表实体。
 */
@Data
@TableName("chat_message")
public class ChatMessageEntity {

    /** 消息主键ID。 */
    @TableId(value = "message_id", type = IdType.AUTO)
    private Long messageId;
    /** 会话ID。 */
    private String sessionId;
    /** 消息角色（user/assistant）。 */
	private ChatMessageRole role;
    /** 消息内容。 */
    private String content;
    /** 本次消息消耗的Token数。 */
    private Integer tokenUsage;
    /** 本次消息耗时（毫秒）。 */
    private Integer latencyMs;
    /** 链路追踪ID。 */
    private String traceId;
    /** 创建时间。 */
    private LocalDateTime createdAt;
}
