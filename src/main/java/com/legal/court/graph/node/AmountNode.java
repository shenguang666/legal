package com.legal.court.graph.node;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.neo4j.core.schema.Node;

import java.math.BigDecimal;

/**
 * 智能小法庭金额节点。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Node("Amount")
public class AmountNode extends BaseCourtNode {

    /** 金额数值。 */
    private BigDecimal amount;
    /** 币种。 */
    private String currency;
    /** 金额原文。 */
    private String rawText;
    /** 金额用途。 */
    private String purpose;
    /** 来源业务ID。 */
    private String sourceBusinessId;
}
