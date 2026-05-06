package com.legal.court.graph.node;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * 智能小法庭证据缺口节点。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Node("Gap")
public class GapNode extends BaseCourtNode {

    /** 缺口类型。 */
    private String gapType;
    /** 缺口严重等级。 */
    private String severity;
    /** 缺失证据或事实描述。 */
    private String missingDescription;
    /** 推荐补充材料 JSON 数组。 */
    private String recommendedMaterialsJson;
    /** 关联诉求业务ID。 */
    private String claimBusinessId;
}
