package com.legal.chat.memory;

import com.legal.config.RagProperties;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

public class ChatMemoryFactory {

    private final RagProperties ragProperties;
    private final ChatMemoryStoreImpl chatMemoryStore;

    public ChatMemoryFactory(RagProperties ragProperties, ChatMemoryStoreImpl chatMemoryStore) {
        this.ragProperties = ragProperties;
        this.chatMemoryStore = chatMemoryStore;
    }

    public ChatMemory create(String sessionId) {
        return MessageWindowChatMemory.builder()
                .id(sessionId)
                .maxMessages(ragProperties.getMaxMemoryMessages())
                .chatMemoryStore(chatMemoryStore)
                .build();
    }
}
