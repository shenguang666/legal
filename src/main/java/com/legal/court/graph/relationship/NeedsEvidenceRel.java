package com.legal.court.graph.relationship;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;

/**
 * 需要补证关系属性，表示诉求、抗辩或风险需要额外证据补强。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@RelationshipProperties
public class NeedsEvidenceRel extends BaseCourtRelationship {

    /** 缺失证据类型。 */
    private String missingEvidenceType;
    /** 补证优先级。 */
    private String priority;
    /** 补证说明。 */
    private String missingDescription;
}
