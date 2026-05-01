package com.legal.contract.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.legal.enums.ContractRiskSeverity;
import com.legal.enums.ContractRuleType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("contract_rule_definition")
public class ContractRuleDefinitionEntity {

    /** 规则定义主键ID。 */
    @TableId(value = "rule_id", type = IdType.AUTO)
    private Long ruleId;
    /** 规则所属租户ID，0 表示平台默认规则。 */
    private Long tenantId;
    /** 规则编码，用于程序识别和去重。 */
    private String ruleCode;
    /** 规则显示名称。 */
    private String ruleName;
    /** 规则类型，用于匹配具体校验器。 */
    private ContractRuleType ruleType;
    /** 规则来源类型（结构化/手工录入/导入文档）。 */
    private String ruleSourceType;
    /** 规则主要作用的字段编码。 */
    private String fieldCode;
    /** 关联的风险规则文档ID。 */
    private Long documentId;
    /** 规则命中后的严重级别。 */
    private ContractRiskSeverity severity;
    /** 规则是否启用。 */
    private Boolean enabled;
    /** 检索型规则的命中阈值。 */
    private BigDecimal hitThreshold;
    /** 手工录入的规则原文。 */
    private String ruleContent;
    /** 规则参数，通常为 JSON 字符串。 */
    private String ruleParams;
    /** 规则执行或展示排序。 */
    private Integer sortOrder;
    /** 规则创建时间。 */
    private LocalDateTime createdAt;
    /** 规则最近更新时间。 */
    private LocalDateTime updatedAt;
}
