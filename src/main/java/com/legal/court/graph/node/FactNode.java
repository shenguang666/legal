package com.legal.court.graph.node;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * 智能小法庭事实节点。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Node("Fact")
public class FactNode extends BaseCourtNode {

    /** 事实内容。 */
    private String content;
    /** 事实状态。 */
    private String factStatus;
    /** 事实发生时间文本。 */
    private String occurredAtText;
    /** 事实来源摘要。 */
    private String sourceSummary;
    /** 事实置信度。 */
    private Double confidence;
}
