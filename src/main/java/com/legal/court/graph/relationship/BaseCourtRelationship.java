package com.legal.court.graph.relationship;

import com.legal.court.graph.node.BaseCourtNode;
import lombok.Data;
import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.TargetNode;

import java.time.LocalDateTime;

/**
 * 智能小法庭 Neo4j 图谱关系公共属性。
 */
@Data
public abstract class BaseCourtRelationship {

    /** Neo4j 关系内部主键。 */
    @RelationshipId
    private Long relationshipId;
    /** 关系目标节点。 */
    @TargetNode
    private BaseCourtNode targetNode;
    /** 租户ID。 */
    private Long tenantId;
    /** 案件ID。 */
    private Long caseId;
    /** 关系业务ID，用于事件投影和幂等合并。 */
    private String businessId;
    /** 来源图谱事件ID。 */
    private Long eventId;
    /** 证据来源业务ID。 */
    private String evidenceSourceBusinessId;
    /** 来源证据登记ID。 */
    private Long sourceEvidenceId;
    /** 来源父切片ID。 */
    private Long parentChunkId;
    /** 来源子切片ID。 */
    private Long childChunkId;
    /** 关系置信度。 */
    private Double confidence;
    /** 关系推理依据。 */
    private String rationale;
    /** 创建时间。 */
    private LocalDateTime createdAt;
    /** 更新时间。 */
    private LocalDateTime updatedAt;
}
