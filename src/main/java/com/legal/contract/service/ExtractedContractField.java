package com.legal.contract.service;

import com.legal.enums.ContractFieldStatus;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ExtractedContractField {

    private String fieldCode;
    private String fieldName;
    private String rawValue;
    private String normalizedValue;
    private ContractFieldStatus status;
    private BigDecimal confidence;
    private String evidenceText;
    private String sourceChunkRef;
    private String extractorType;
    private Integer fieldOrder;
    private String groupKey;
    private String explanation;
}
