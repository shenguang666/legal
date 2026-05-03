package com.legal.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.legal.enums.QaKnowledgeIndexScope;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 智能问答知识库索引配置实体。
 */
@Data
@TableName("knowledge_qa_index_config")
public class KnowledgeQaIndexConfigEntity {

    /** 配置主键ID。 */
    @TableId(value = "config_id", type = IdType.AUTO)
    private Long configId;
    /** 租户ID，用于隔离不同租户的智能问答检索配置。 */
    private Long tenantId;
    /** 智能问答知识库检索索引范围。 */
    private QaKnowledgeIndexScope indexScope;
    /** 创建配置的用户ID。 */
    private Long createdBy;
    /** 最近更新配置的用户ID。 */
    private Long updatedBy;
    /** 创建时间。 */
    private LocalDateTime createdAt;
    /** 更新时间。 */
    private LocalDateTime updatedAt;
}
