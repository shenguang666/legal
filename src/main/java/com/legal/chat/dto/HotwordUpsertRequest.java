package com.legal.chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class HotwordUpsertRequest {

    @NotBlank
    private String requestId;
    private String hotwordKey;
    @NotBlank
    private String content;
    @NotBlank
    private String presetAnswer;
    private String category;
    private Integer weight;
    private Integer sortOrder;
    private Boolean enabled;
}
