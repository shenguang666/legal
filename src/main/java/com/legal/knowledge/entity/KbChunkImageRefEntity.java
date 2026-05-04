package com.legal.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库切片图片引用实体。
 */
@Data
@TableName("kb_chunk_image_ref")
public class KbChunkImageRefEntity {

    /** 切片图片引用主键ID。 */
    @TableId(value = "chunk_image_ref_id", type = IdType.AUTO)
    private Long chunkImageRefId;
    /** 租户ID。 */
    private Long tenantId;
    /** 切片ID。 */
    private Long chunkId;
    /** 图片资产ID。 */
    private Long imageAssetId;
    /** 图片在切片中的出现顺序。 */
    private Integer imageOrder;
    /** 创建时间。 */
    private LocalDateTime createdAt;
}
