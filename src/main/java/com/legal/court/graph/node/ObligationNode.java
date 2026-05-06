package com.legal.court.graph.node;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * 智能小法庭合同义务节点。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Node("Obligation")
public class ObligationNode extends BaseCourtNode {

    /** 义务主体角色。 */
    private String obligorRole;
    /** 权利主体角色。 */
    private String obligeeRole;
    /** 义务内容。 */
    private String content;
    /** 履行期限文本。 */
    private String dueDateText;
    /** 义务来源条款业务ID。 */
    private String sourceClauseBusinessId;
}
