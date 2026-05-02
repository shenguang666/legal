package com.legal.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class HotwordStatusUpdateRequest {

    @NotBlank
    private String requestId;
    @NotNull
    private Boolean enabled;
}
