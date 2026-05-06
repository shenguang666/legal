package com.legal.court.graph.node;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * 智能小法庭诉讼风险节点。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Node("Risk")
public class RiskNode extends BaseCourtNode {

    /** 风险类型。 */
    private String riskType;
    /** 风险等级。 */
    private String severity;
    /** 风险描述。 */
    private String description;
    /** 风险成因。 */
    private String rationale;
    /** 关联诉求业务ID。 */
    private String claimBusinessId;
}
