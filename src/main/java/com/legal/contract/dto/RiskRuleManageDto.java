package com.legal.contract.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RiskRuleManageDto {

    private Long ruleId;
    private String ruleCode;
    private String ruleName;
    private String ruleType;
    private String ruleSourceType;
    private String severity;
    private Boolean enabled;
    private Double hitThreshold;
    private Long documentId;
    private String documentTitle;
    private String documentSource;
    private String documentStatus;
    private String documentIndexStatus;
    private String documentParseMethod;
    private String documentParseStatus;
    private String documentParseFailureReason;
    private String ruleContent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
