package com.legal.court.graph.node;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * 智能小法庭模拟裁判观点节点。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Node("JudgmentPoint")
public class JudgmentPointNode extends BaseCourtNode {

    /** 模拟裁判报告ID。 */
    private Long reportId;
    /** 裁判观点类型。 */
    private String pointType;
    /** 裁判观点内容。 */
    private String content;
    /** 对原告是否不利。 */
    private Boolean unfavorableToPlaintiff;
    /** 对被告是否不利。 */
    private Boolean unfavorableToDefendant;
    /** 裁判观点依据摘要。 */
    private String rationale;
}
