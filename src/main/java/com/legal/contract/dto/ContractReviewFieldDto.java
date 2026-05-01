package com.legal.contract.dto;

import lombok.Data;

@Data
public class ContractReviewFieldDto {

    private Long fieldId;
    private String fieldCode;
    private String fieldName;
    private String rawValue;
    private String normalizedValue;
    private String status;
    private Double confidence;
    private String evidenceText;
    private String sourceChunkRef;
    private String extractorType;
    private Integer fieldOrder;
    private String groupKey;
    private String explanation;
}
