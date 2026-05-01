package com.legal.contract.service;

import com.legal.enums.ContractRiskSeverity;
import com.legal.enums.ContractRuleExecutionStatus;
import com.legal.enums.ContractRuleType;
import lombok.Data;

@Data
public class ContractRuleEvaluation {

    private String ruleCode;
    private String ruleName;
    private ContractRuleType ruleType;
    private ContractRiskSeverity severity;
    private ContractRuleExecutionStatus executionStatus;
    private String message;
    private String evidenceText;
    private String affectedFieldCodes;
}
