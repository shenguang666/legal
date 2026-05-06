package com.legal.court.graph.node;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * 智能小法庭文档节点。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Node("Document")
public class DocumentNode extends BaseCourtNode {

    /** 文档ID。 */
    private Long documentId;
    /** 文档名称。 */
    private String documentName;
    /** 文档业务类型。 */
    private String bizType;
    /** 文档解析方式。 */
    private String parseMethod;
    /** 文档地址。 */
    private String documentUrl;
}
