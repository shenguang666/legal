package com.legal.court.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 智能小法庭证据链图谱快照 DTO。
 */
@Data
public class CourtGraphSnapshotDto {

    /** 租户ID。 */
    private Long tenantId;
    /** 案件ID。 */
    private Long caseId;
    /** 图谱 schema 版本号。 */
    private Integer schemaVersion;
    /** 图谱节点列表。 */
    private List<NodeDto> nodes;
    /** 图谱关系列表。 */
    private List<EdgeDto> edges;
    /** 图谱节点总数。 */
    private Integer nodeCount;
    /** 图谱关系总数。 */
    private Integer edgeCount;
    /** 图谱是否因前端限制被裁剪。 */
    private Boolean truncated;
    /** 图谱状态：READY / PROJECTING / UNAVAILABLE。 */
    private String graphState;
    /** 剩余待投影事件数量。 */
    private Long pendingEventCount;
    /** 降级或裁剪提示。 */
    private String warning;

    /** 图谱节点 DTO。 */
    @Data
    public static class NodeDto {

        /** 节点业务ID。 */
        private String businessId;
        /** 节点类型。 */
        private String type;
        /** 节点展示名称。 */
        private String label;
        /** 节点状态。 */
        private String status;
        /** 节点属性。 */
        private Map<String, Object> properties;
    }

    /** 图谱关系 DTO。 */
    @Data
    public static class EdgeDto {

        /** 关系业务ID。 */
        private String businessId;
        /** 起点节点业务ID。 */
        private String sourceBusinessId;
        /** 终点节点业务ID。 */
        private String targetBusinessId;
        /** 关系类型。 */
        private String type;
        /** 关系展示名称。 */
        private String label;
        /** 关系属性。 */
        private Map<String, Object> properties;
    }
}
