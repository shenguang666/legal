package com.legal.chat.memory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.legal.chat.entity.ChatMessageEntity;
import com.legal.chat.mapper.ChatMessageMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

import java.util.ArrayList;
import java.util.List;

public class ChatMemoryStoreImpl implements ChatMemoryStore {

    private final ChatMessageMapper chatMessageMapper;

    public ChatMemoryStoreImpl(ChatMessageMapper chatMessageMapper) {
        this.chatMessageMapper = chatMessageMapper;
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        List<ChatMessageEntity> entities = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessageEntity>()
                        .eq(ChatMessageEntity::getSessionId, String.valueOf(memoryId))
                        .orderByAsc(ChatMessageEntity::getCreatedAt)
        );
        List<ChatMessage> messages = new ArrayList<>();
        for (ChatMessageEntity entity : entities) {
            messages.add(toChatMessage(entity));
        }
        return messages;
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
    }

    @Override
    public void deleteMessages(Object memoryId) {
    }

    private ChatMessage toChatMessage(ChatMessageEntity entity) {
        if ("assistant".equalsIgnoreCase(entity.getRole())) {
            return AiMessage.from(entity.getContent());
        }
        if ("system".equalsIgnoreCase(entity.getRole())) {
            return SystemMessage.from(entity.getContent());
        }
        return UserMessage.from(entity.getContent());
    }
}
