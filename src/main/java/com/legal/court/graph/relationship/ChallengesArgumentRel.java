package com.legal.court.graph.relationship;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;

/**
 * 挑战观点关系属性，表示某个庭审观点反驳另一观点。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@RelationshipProperties
public class ChallengesArgumentRel extends BaseCourtRelationship {

    /** 发起挑战的观点ID。 */
    private Long challengerArgumentId;
    /** 被挑战的观点ID。 */
    private Long challengedArgumentId;
    /** 反驳理由摘要。 */
    private String challengeReason;
}
