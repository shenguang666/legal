package com.legal.court.graph.node;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * 智能小法庭庭审观点节点。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Node("Argument")
public class ArgumentNode extends BaseCourtNode {

    /** MySQL 观点ID。 */
    private Long argumentId;
    /** 庭审轮次ID。 */
    private Long roundId;
    /** 发言角色。 */
    private String speakerRole;
    /** 发言所代表的当事人角色。 */
    private String speakerParty;
    /** 观点立场。 */
    private String stance;
    /** 观点正文。 */
    private String content;
}
