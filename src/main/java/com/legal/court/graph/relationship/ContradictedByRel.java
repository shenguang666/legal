package com.legal.court.graph.relationship;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;

/**
 * 反驳关系属性，表示事实、诉求或观点被其他证据反驳。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@RelationshipProperties
public class ContradictedByRel extends BaseCourtRelationship {

    /** 反驳强度。 */
    private String contradictionLevel;
    /** 矛盾点摘要。 */
    private String contradictionSummary;
}
