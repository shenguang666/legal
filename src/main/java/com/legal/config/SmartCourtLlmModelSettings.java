package com.legal.config;

import java.time.Duration;

public record SmartCourtLlmModelSettings(
        String baseUrl,
        String apiKey,
        String modelName,
        Double temperature,
        Integer maxOutputTokens,
        Duration timeout,
        boolean dedicatedApiKey
) {
}
