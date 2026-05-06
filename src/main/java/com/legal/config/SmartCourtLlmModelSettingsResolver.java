package com.legal.config;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SmartCourtLlmModelSettingsResolver {

    public SmartCourtLlmModelSettings resolve(SmartCourtProperties smartCourtProperties,
                                              OpenAiChatModelProperties fallbackProperties) {
        SmartCourtProperties.Llm llm = smartCourtProperties == null ? null : smartCourtProperties.getLlm();
        boolean dedicatedApiKey = llm != null && StringUtils.hasText(llm.getApiKey());
        return new SmartCourtLlmModelSettings(
                textOrDefault(llm == null ? null : llm.getBaseUrl(), fallbackProperties.getBaseUrl()),
                textOrDefault(llm == null ? null : llm.getApiKey(), fallbackProperties.getApiKey()),
                textOrDefault(llm == null ? null : llm.getModelName(), fallbackProperties.getModelName()),
                llm == null || llm.getTemperature() == null ? fallbackProperties.getTemperature() : llm.getTemperature(),
                llm == null || llm.getMaxOutputTokens() == null ? fallbackProperties.getMaxOutputTokens() : llm.getMaxOutputTokens(),
                llm == null || llm.getTimeout() == null ? fallbackProperties.getTimeout() : llm.getTimeout(),
                dedicatedApiKey
        );
    }

    public SmartCourtLlmModelSettings resolveStreaming(SmartCourtProperties smartCourtProperties,
                                                       OpenAiStreamingChatModelProperties fallbackProperties) {
        SmartCourtProperties.Llm llm = smartCourtProperties == null ? null : smartCourtProperties.getLlm();
        boolean dedicatedApiKey = llm != null && StringUtils.hasText(llm.getApiKey());
        return new SmartCourtLlmModelSettings(
                textOrDefault(llm == null ? null : llm.getBaseUrl(), fallbackProperties.getBaseUrl()),
                textOrDefault(llm == null ? null : llm.getApiKey(), fallbackProperties.getApiKey()),
                textOrDefault(llm == null ? null : llm.getModelName(), fallbackProperties.getModelName()),
                llm == null || llm.getTemperature() == null ? fallbackProperties.getTemperature() : llm.getTemperature(),
                llm == null || llm.getMaxOutputTokens() == null ? fallbackProperties.getMaxOutputTokens() : llm.getMaxOutputTokens(),
                llm == null || llm.getTimeout() == null ? fallbackProperties.getTimeout() : llm.getTimeout(),
                dedicatedApiKey
        );
    }

    private String textOrDefault(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}
