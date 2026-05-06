package com.legal.court.graph;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 智能小法庭图谱事件载荷。
 */
@Data
public class CourtGraphEventPayload {

    /** 节点标签。 */
    private String label;
    /** 关系类型。 */
    private String relationType;
    /** 节点或关系业务ID。 */
    private String businessId;
    /** 起点节点标签。 */
    private String fromLabel;
    /** 起点节点业务ID。 */
    private String fromBusinessId;
    /** 终点节点标签。 */
    private String toLabel;
    /** 终点节点业务ID。 */
    private String toBusinessId;
    /** 节点或关系属性。 */
    private Map<String, Object> properties = new HashMap<>();
}
