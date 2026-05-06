package com.legal.court.graph.relationship;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;

/**
 * 支持模拟裁判观点关系属性，表示事实、证据或法源依据支持裁判观点。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@RelationshipProperties
public class SupportsJudgmentRel extends BaseCourtRelationship {

    /** 模拟裁判报告ID。 */
    private Long reportId;
    /** 裁判观点业务ID。 */
    private String judgmentPointBusinessId;
    /** 支持裁判观点的理由摘要。 */
    private String judgmentReason;
}
