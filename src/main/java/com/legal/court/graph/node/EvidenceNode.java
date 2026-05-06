package com.legal.court.graph.node;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * 智能小法庭证据节点。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Node("Evidence")
public class EvidenceNode extends BaseCourtNode {

    /** MySQL 证据登记ID。 */
    private Long evidenceId;
    /** 关联文档ID。 */
    private Long documentId;
    /** 证据角色。 */
    private String evidenceRole;
    /** 证据状态。 */
    private String evidenceStatus;
    /** 证据摘要。 */
    private String summary;
}
