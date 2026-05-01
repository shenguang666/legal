package com.legal.contract.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("contract_field_definition")
public class ContractFieldDefinitionEntity {

    /** 字段定义主键ID。 */
    @TableId(value = "field_definition_id", type = IdType.AUTO)
    private Long fieldDefinitionId;
    /** 所属租户ID，0 表示平台默认字段定义。 */
    private Long tenantId;
    /** 字段编码，如 party_a、contract_amount。 */
    private String fieldCode;
    /** 字段显示名称。 */
    private String fieldName;
    /** 抽取器类型（PARTY_PATTERN/AMOUNT_PATTERN/DATE_KEYWORD/KEYWORD_LINE）。 */
    private String extractorKind;
    /** 当前字段的正则表达式配置。 */
    private String patternExpr;
    /** 当前字段的关键字配置，使用换行或逗号分隔。 */
    private String keywordConfig;
    /** 是否允许抽取多个结果。 */
    private Boolean repeatable;
    /** 是否按归一化结果去重。 */
    private Boolean deduplicateByNormalized;
    /** 字段定义是否启用。 */
    private Boolean enabled;
    /** 字段抽取顺序。 */
    private Integer sortOrder;
    /** 字段用途或规则说明。 */
    private String description;
    /** 创建时间。 */
    private LocalDateTime createdAt;
    /** 最近更新时间。 */
    private LocalDateTime updatedAt;
}
