package com.legal.court.graph.node;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * 智能小法庭合同条款节点。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Node("Clause")
public class ClauseNode extends BaseCourtNode {

    /** 条款编号。 */
    private String clauseNo;
    /** 条款标题。 */
    private String title;
    /** 条款正文。 */
    private String content;
    /** 所属文档ID。 */
    private Long documentId;
    /** 来源切片ID。 */
    private Long chunkId;
}
