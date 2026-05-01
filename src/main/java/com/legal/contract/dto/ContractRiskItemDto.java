package com.legal.contract.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ContractRiskItemDto {

    private Long riskId;
    private String ruleCode;
    private String ruleName;
    private String ruleType;
    private String severity;
    private String executionStatus;
    private String message;
    private String evidenceText;
    private String affectedFieldCodes;
    private LocalDateTime createdAt;
}
