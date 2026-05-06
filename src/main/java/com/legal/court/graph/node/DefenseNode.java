package com.legal.court.graph.node;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * 智能小法庭抗辩节点。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Node("Defense")
public class DefenseNode extends BaseCourtNode {

    /** 提出抗辩的一方角色。 */
    private String defenderRole;
    /** 抗辩类型。 */
    private String defenseType;
    /** 抗辩内容。 */
    private String content;
    /** 抗辩依据摘要。 */
    private String basisSummary;
    /** 被抗辩的诉求业务ID。 */
    private String targetClaimBusinessId;
}
