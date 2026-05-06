package com.legal.court.graph.node;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * 智能小法庭法源依据节点。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Node("LegalBasis")
public class LegalBasisNode extends BaseCourtNode {

    /** 法源名称。 */
    private String sourceName;
    /** 条文编号。 */
    private String articleNo;
    /** 条文标题。 */
    private String title;
    /** 条文内容。 */
    private String content;
    /** 法源层级。 */
    private String sourceLevel;
}
