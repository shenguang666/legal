package com.legal.court.graph.node;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * 智能小法庭文档切片节点。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Node("Chunk")
public class ChunkNode extends BaseCourtNode {

    /** 切片ID。 */
    private Long chunkId;
    /** 父切片ID。 */
    private Long parentChunkId;
    /** 所属文档ID。 */
    private Long documentId;
    /** 切片序号。 */
    private Integer chunkIndex;
    /** 切片内容摘要。 */
    private String contentPreview;
}
