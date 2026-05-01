package com.legal.contract.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.legal.enums.ContractFieldStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("contract_review_field")
public class ContractReviewFieldEntity {

    /** 字段结果主键ID。 */
    @TableId(value = "field_id", type = IdType.AUTO)
    private Long fieldId;
    /** 所属合同审阅记录ID。 */
    private Long reviewId;
    /** 字段编码，如 party_a、contract_amount。 */
    private String fieldCode;
    /** 字段显示名称，便于前端直接展示。 */
    private String fieldName;
    /** 文档中原始识别值。 */
    private String rawValue;
    /** 归一化后的字段值，便于规则比较和查询。 */
    private String normalizedValue;
    /** 字段抽取状态（已抽取/缺失/不确定）。 */
    private ContractFieldStatus status;
    /** 字段抽取置信度。 */
    private BigDecimal confidence;
    /** 支撑该字段识别结果的证据片段。 */
    private String evidenceText;
    /** 证据来源切片标识。 */
    private String sourceChunkRef;
    /** 抽取器类型，如 pattern、semantic。 */
    private String extractorType;
    /** 字段在审阅详情中的展示顺序。 */
    private Integer fieldOrder;
    /** 多值字段的分组键。 */
    private String groupKey;
    /** 对缺失或不确定状态的解释信息。 */
    private String explanation;
    /** 字段结果创建时间。 */
    private LocalDateTime createdAt;
}
