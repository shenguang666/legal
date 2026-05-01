package com.legal.contract.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ContractReviewDetailDto {

    private Long reviewId;
    private Long documentId;
    private Integer docVersion;
    private String status;
    private String riskLevel;
    private Integer riskCount;
    private Integer hitRuleCount;
    private Integer totalFieldCount;
    private Integer extractedFieldCount;
    private Integer missingFieldCount;
    private String summaryText;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private List<ContractReviewFieldDto> fields;
    private List<ContractRiskItemDto> riskItems;
}
