package com.legal.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库切片表实体。
 */
@Data
@TableName("kb_chunk")
public class KbChunkEntity {

    /** 切片主键ID。 */
    @TableId(value = "chunk_id", type = IdType.AUTO)
    private Long chunkId;
    /** 租户ID。 */
    private Long tenantId;
    /** 文档ID。 */
    private Long documentId;
    /** 文档版本号。 */
    private Integer docVersion;
    /** 切片顺序号。 */
    private Integer chunkOrder;
    /** 切片内容。 */
    private String content;
    /** 切片内容哈希。 */
    private String contentHash;
    /** 创建时间。 */
    private LocalDateTime createdAt;
}
