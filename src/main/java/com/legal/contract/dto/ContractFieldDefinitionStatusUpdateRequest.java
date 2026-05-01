package com.legal.contract.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ContractFieldDefinitionStatusUpdateRequest {

    @NotBlank
    private String requestId;
    @NotNull
    private Boolean enabled;
}
