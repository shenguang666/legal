package com.legal.contract.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.legal.enums.ContractRiskSeverity;
import com.legal.enums.ContractRuleExecutionStatus;
import com.legal.enums.ContractRuleType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("contract_risk_item")
public class ContractRiskItemEntity {

    /** 风险项主键ID。 */
    @TableId(value = "risk_id", type = IdType.AUTO)
    private Long riskId;
    /** 所属合同审阅记录ID。 */
    private Long reviewId;
    /** 命中的规则编码。 */
    private String ruleCode;
    /** 命中的规则名称。 */
    private String ruleName;
    /** 规则类型。 */
    private ContractRuleType ruleType;
    /** 风险严重级别。 */
    private ContractRiskSeverity severity;
    /** 规则执行结果状态（命中/通过/跳过）。 */
    private ContractRuleExecutionStatus executionStatus;
    /** 风险说明或规则命中提示。 */
    private String message;
    /** 风险项对应的证据片段。 */
    private String evidenceText;
    /** 受该风险影响的字段编码列表。 */
    private String affectedFieldCodes;
    /** 风险项创建时间。 */
    private LocalDateTime createdAt;
}
