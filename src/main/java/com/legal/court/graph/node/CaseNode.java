package com.legal.court.graph.node;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * 智能小法庭案件节点。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Node("Case")
public class CaseNode extends BaseCourtNode {

    /** 案件标题。 */
    private String title;
    /** 案件类型。 */
    private String caseType;
    /** 用户诉讼立场。 */
    private String userSide;
    /** 案件简要描述。 */
    private String caseSummary;
}
