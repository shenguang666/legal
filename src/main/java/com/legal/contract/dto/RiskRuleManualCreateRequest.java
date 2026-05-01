package com.legal.contract.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RiskRuleManualCreateRequest {

    @NotBlank
    private String requestId;
    private String ruleCode;
    @NotBlank
    private String ruleName;
    private String source;
    private String severity;
    private Double hitThreshold;
    @NotBlank
    private String ruleContent;
}
