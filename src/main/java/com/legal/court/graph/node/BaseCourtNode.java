package com.legal.court.graph.node;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.Id;

import java.time.LocalDateTime;

/**
 * 智能小法庭 Neo4j 图谱节点公共属性。
 */
@Data
public abstract class BaseCourtNode {

    /** Neo4j 节点主键，建议由 tenantId、节点类型和 businessId 拼接生成。 */
    @Id
    private String graphId;
    /** 租户ID。 */
    private Long tenantId;
    /** 案件ID。 */
    private Long caseId;
    /** 节点业务ID，用于事件投影和幂等合并。 */
    private String businessId;
    /** 图谱 schema 版本号。 */
    private Integer schemaVersion;
    /** 节点展示名称。 */
    private String label;
    /** 节点状态。 */
    private String status;
    /** 来源事件ID。 */
    private Long eventId;
    /** 创建时间。 */
    private LocalDateTime createdAt;
    /** 更新时间。 */
    private LocalDateTime updatedAt;
}
