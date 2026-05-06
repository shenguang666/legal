package com.legal.court.graph.relationship;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;

/**
 * 支持观点关系属性，表示事实或证据支持某个庭审观点。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@RelationshipProperties
public class SupportsArgumentRel extends BaseCourtRelationship {

    /** 被支持观点ID。 */
    private Long argumentId;
    /** 支持理由摘要。 */
    private String supportReason;
}
