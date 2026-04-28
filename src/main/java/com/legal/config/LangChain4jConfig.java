package com.legal.config;

import com.legal.chat.mapper.ChatMessageMapper;
import com.legal.chat.memory.ChatMemoryFactory;
import com.legal.chat.memory.ChatMemoryStoreImpl;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties({
        RagProperties.class,
        OpenAiChatModelProperties.class,
        OpenAiEmbeddingProperties.class,
        ElasticsearchProperties.class
})
public class LangChain4jConfig {

    @Bean
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
    public ChatMemoryStoreImpl chatMemoryStore(ChatMessageMapper chatMessageMapper) {
        return new ChatMemoryStoreImpl(chatMessageMapper);
    }

    @Bean
    public ChatMemoryFactory chatMemoryFactory(RagProperties ragProperties,
                                               ChatMemoryStoreImpl chatMemoryStore) {
        return new ChatMemoryFactory(ragProperties, chatMemoryStore);
    }
}
