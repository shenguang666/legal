package com.legal.memory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户外挂知识库表实体。
 */
@Data
@TableName("user_knowledge")
public class UserKnowledgeEntity {

    /** 用户知识主键ID。 */
    @TableId(value = "knowledge_id", type = IdType.AUTO)
    private Long knowledgeId;
    /** 租户ID。 */
    private Long tenantId;
    /** 用户ID。 */
    private Long userId;
    /** 来源会话ID。 */
    private String sessionId;
    /** 用户问题。 */
    private String question;
    /** 助手回答。 */
    private String answer;
    /** 用于索引的知识内容。 */
    private String content;
    /** 知识来源说明。 */
    private String source;
    /** 知识等级（MUST/OPTIONAL/FORBIDDEN）。 */
    private String knowledgeLevel;
    /** 分类理由。 */
    private String reason;
    /** 提炼后的核心内容。 */
    private String coreContent;
    /** 状态（PENDING/ACTIVE/REJECTED/DELETED）。 */
    private String status;
    /** 索引状态（PENDING/COMPLETED/FAILED）。 */
    private String indexStatus;
    /** 来源用户消息ID。 */
    private Long sourceUserMessageId;
    /** 来源助手消息ID。 */
    private Long sourceAssistantMessageId;
    /** 创建时间。 */
    private LocalDateTime createdAt;
    /** 更新时间。 */
    private LocalDateTime updatedAt;
    /** 复核时间。 */
    private LocalDateTime reviewedAt;
}
