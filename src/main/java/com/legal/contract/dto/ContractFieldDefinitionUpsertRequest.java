package com.legal.contract.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ContractFieldDefinitionUpsertRequest {

    @NotBlank
    private String requestId;
    @NotBlank
    private String fieldCode;
    @NotBlank
    private String fieldName;
    @NotBlank
    private String extractorKind;
    private String patternExpr;
    private String keywordConfig;
    private Boolean repeatable;
    private Boolean deduplicateByNormalized;
    private Boolean enabled;
    private Integer sortOrder;
    private String description;
}
