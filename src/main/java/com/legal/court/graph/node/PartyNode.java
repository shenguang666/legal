package com.legal.court.graph.node;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * 智能小法庭当事人节点。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Node("Party")
public class PartyNode extends BaseCourtNode {

    /** 当事人角色。 */
    private String partyRole;
    /** 当事人显示名称。 */
    private String displayName;
    /** 当事人简介或主体信息。 */
    private String description;
    /** 是否为用户所代表的一方。 */
    private Boolean userSide;
}
