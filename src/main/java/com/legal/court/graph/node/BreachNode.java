package com.legal.court.graph.node;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * 智能小法庭违约行为节点。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Node("Breach")
public class BreachNode extends BaseCourtNode {

    /** 违约主体角色。 */
    private String breachingRole;
    /** 违约类型。 */
    private String breachType;
    /** 违约行为描述。 */
    private String content;
    /** 违约发生时间文本。 */
    private String occurredAtText;
    /** 被违反的义务业务ID。 */
    private String obligationBusinessId;
}
