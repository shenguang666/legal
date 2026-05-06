package com.legal.court.graph.relationship;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;

/**
 * 支持关系属性，表示事实、诉求或观点被证据支持。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@RelationshipProperties
public class SupportedByRel extends BaseCourtRelationship {

    /** 支持强度。 */
    private String supportLevel;
    /** 被支持对象的业务类型。 */
    private String supportedObjectType;
}
