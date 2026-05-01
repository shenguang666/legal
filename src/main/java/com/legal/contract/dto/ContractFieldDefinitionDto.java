package com.legal.contract.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ContractFieldDefinitionDto {

    private Long fieldDefinitionId;
    private Long tenantId;
    private boolean systemDefault;
    private String fieldCode;
    private String fieldName;
    private String extractorKind;
    private String patternExpr;
    private String keywordConfig;
    private Boolean repeatable;
    private Boolean deduplicateByNormalized;
    private Boolean enabled;
    private Integer sortOrder;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
