package com.legal.court.graph.node;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * 智能小法庭诉求节点。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Node("Claim")
public class ClaimNode extends BaseCourtNode {

    /** 提出诉求的一方角色。 */
    private String claimantRole;
    /** 诉求类型。 */
    private String claimType;
    /** 诉求内容。 */
    private String content;
    /** 诉求金额。 */
    private String amountText;
    /** 诉求依据摘要。 */
    private String basisSummary;
}
