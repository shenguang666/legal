package com.legal.court.graph.relationship;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;

/**
 * 来源关系属性，表示节点从文档、切片或条款中抽取而来。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@RelationshipProperties
public class DerivedFromRel extends BaseCourtRelationship {

    /** 来源类型。 */
    private String sourceType;
    /** 来源原文摘录。 */
    private String sourceExcerpt;
}
