package com.legal.court.graph.node;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.neo4j.core.schema.Node;

import java.time.LocalDate;

/**
 * 智能小法庭日期节点。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Node("Date")
public class DateNode extends BaseCourtNode {

    /** 日期值。 */
    private LocalDate dateValue;
    /** 日期原文。 */
    private String rawText;
    /** 日期类型。 */
    private String dateType;
    /** 日期精度。 */
    private String precision;
    /** 来源业务ID。 */
    private String sourceBusinessId;
}
