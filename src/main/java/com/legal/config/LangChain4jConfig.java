package com.legal.config;

import com.legal.chat.mapper.ChatMessageMapper;
import com.legal.chat.memory.ChatMemoryFactory;
import com.legal.chat.memory.ChatMemoryStoreImpl;
import com.legal.config.LegalMemoryProperties;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties({
        RagProperties.class,
        OpenAiChatModelProperties.class,
        OpenAiStreamingChatModelProperties.class,
        OpenAiEmbeddingProperties.class,
        ElasticsearchProperties.class,
        LegalMemoryProperties.class,
        ContractReviewProperties.class,
        RagRetrievalMetricProperties.class,
        TokenUsageMetricProperties.class,
        DocumentProcessingProperties.class,
        OssStorageProperties.class,
        SmartCourtProperties.class
})
public class LangChain4jConfig {

    @Bean
    @Primary
    public ChatModel chatModel(OpenAiChatModelProperties properties) {
        if (!StringUtils.hasText(properties.getApiKey())) {
            return null;
        }
        return OpenAiChatModel.builder()
                .baseUrl(properties.getBaseUrl())
                .apiKey(properties.getApiKey())
                .modelName(properties.getModelName())
                .temperature(properties.getTemperature())
                .maxCompletionTokens(properties.getMaxOutputTokens())
                .timeout(properties.getTimeout())
                .logRequests(properties.isLogRequests())
                .logResponses(properties.isLogResponses())
                .build();
    }

    @Bean
    @Primary
    public StreamingChatModel streamingChatModel(OpenAiStreamingChatModelProperties properties) {
        if (!StringUtils.hasText(properties.getApiKey())) {
            return null;
        }
        return OpenAiStreamingChatModel.builder()
                .baseUrl(properties.getBaseUrl())
                .apiKey(properties.getApiKey())
                .modelName(properties.getModelName())
                // 兼容 DeepSeek/Qwen 等 OpenAI 兼容网关的流式 tool_call id 行为
                .accumulateToolCallId(false)
                .temperature(properties.getTemperature())
                .maxCompletionTokens(properties.getMaxOutputTokens())
                .timeout(properties.getTimeout())
                .build();
    }

    @Bean("smartCourtChatModel")
    public ChatModel smartCourtChatModel(SmartCourtProperties smartCourtProperties,
                                         OpenAiChatModelProperties fallbackProperties,
                                         SmartCourtLlmModelSettingsResolver resolver) {
        SmartCourtLlmModelSettings settings = resolver.resolve(smartCourtProperties, fallbackProperties);
        if (!StringUtils.hasText(settings.apiKey())) {
            return null;
        }
        return OpenAiChatModel.builder()
                .baseUrl(settings.baseUrl())
                .apiKey(settings.apiKey())
                .modelName(settings.modelName())
                .temperature(settings.temperature())
                .maxCompletionTokens(settings.maxOutputTokens())
                .timeout(settings.timeout())
                .logRequests(fallbackProperties.isLogRequests())
                .logResponses(fallbackProperties.isLogResponses())
                .build();
    }

    @Bean("smartCourtStreamingChatModel")
    public StreamingChatModel smartCourtStreamingChatModel(SmartCourtProperties smartCourtProperties,
                                                           OpenAiStreamingChatModelProperties fallbackProperties,
                                                           SmartCourtLlmModelSettingsResolver resolver) {
        SmartCourtLlmModelSettings settings = resolver.resolveStreaming(smartCourtProperties, fallbackProperties);
        if (!StringUtils.hasText(settings.apiKey())) {
            return null;
        }
        return OpenAiStreamingChatModel.builder()
                .baseUrl(settings.baseUrl())
                .apiKey(settings.apiKey())
                .modelName(settings.modelName())
                .accumulateToolCallId(false)
                .temperature(settings.temperature())
                .maxCompletionTokens(settings.maxOutputTokens())
                .timeout(settings.timeout())
                .build();
    }

    @Bean
    public ChatMemoryStoreImpl chatMemoryStore(ChatMessageMapper chatMessageMapper) {
        return new ChatMemoryStoreImpl(chatMessageMapper);
    }

    @Bean
    public ChatMemoryFactory chatMemoryFactory(RagProperties ragProperties,
                                               ChatMemoryStoreImpl chatMemoryStore) {
        return new ChatMemoryFactory(ragProperties, chatMemoryStore);
    }
}
