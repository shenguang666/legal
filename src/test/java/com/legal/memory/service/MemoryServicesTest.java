package com.legal.memory.service;

import com.legal.config.LegalMemoryProperties;
import com.legal.config.OpenAiChatModelProperties;
import com.legal.enums.UserMemoryStatus;
import com.legal.memory.entity.UserKnowledgeEntity;
import com.legal.memory.entity.UserMemoryItemEntity;
import com.legal.memory.mapper.UserKnowledgeMapper;
import com.legal.memory.mapper.UserMemoryItemMapper;
import com.legal.retrieval.service.OpenAiEmbeddingClient;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MemoryServicesTest {

    @Test
    void longTermMemoryShouldCanonicalizeAndPromoteAfterRepeatedHits() {
        LegalMemoryProperties props = new LegalMemoryProperties();
        props.getLongTerm().setStableThreshold(2);
        UserMemoryItemMapper mapper = mock(UserMemoryItemMapper.class);
        AtomicReference<UserMemoryItemEntity> holder = new AtomicReference<>();
        when(mapper.selectOne(any())).thenAnswer(i -> holder.get());
        doAnswer(i -> { UserMemoryItemEntity e = i.getArgument(0); e.setId(1L); holder.set(e); return 1; }).when(mapper).insert(any(UserMemoryItemEntity.class));
        doAnswer(i -> { holder.set(i.getArgument(0)); return 1; }).when(mapper).updateById(any(UserMemoryItemEntity.class));

        ChatModel chatModel = new FakeChatModel(
                "[{\"memoryType\":\"profile\",\"memoryKey\":\"职业\",\"memoryValue\":\"律师\",\"sensitivityLevel\":\"P1\",\"confidence\":0.91}]",
                "[{\"memoryType\":\"profile\",\"memoryKey\":\"职业\",\"memoryValue\":\"律师\",\"sensitivityLevel\":\"P1\",\"confidence\":0.91}]");
        OpenAiChatModelProperties modelProps = new OpenAiChatModelProperties();
        modelProps.setModelName("test-model");

        LongTermMemoryExtractionService service = new LongTermMemoryExtractionService(props, mapper, chatModel, modelProps);
        service.extractFromSummary(1L, 2L, "s1", "用户长期稳定从事律师工作");
        assertEquals("profession", holder.get().getMemoryKey());
        assertEquals(UserMemoryStatus.CANDIDATE, holder.get().getStatus());
        assertEquals(1, holder.get().getConfirmationCount());

        service.extractFromSummary(1L, 2L, "s1", "用户持续以律师身份处理案件");
        assertEquals(UserMemoryStatus.ACTIVE, holder.get().getStatus());
        assertEquals(2, holder.get().getConfirmationCount());
        assertNotNull(holder.get().getStableSince());
    }

    @Test
    void optionalKnowledgeShouldBeReviewedBySummaryBeforeActivation() {
        LegalMemoryProperties props = new LegalMemoryProperties();
        props.getUserKnowledge().setMinQuestionLength(1);
        UserKnowledgeMapper mapper = mock(UserKnowledgeMapper.class);
        AtomicReference<UserKnowledgeEntity> holder = new AtomicReference<>();
        when(mapper.selectCount(any())).thenReturn(0L);
        doAnswer(i -> { UserKnowledgeEntity e = i.getArgument(0); e.setKnowledgeId(1L); holder.set(e); return 1; }).when(mapper).insert(any(UserKnowledgeEntity.class));
        doAnswer(i -> { holder.set(i.getArgument(0)); return 1; }).when(mapper).updateById(any(UserKnowledgeEntity.class));
        when(mapper.selectList(any())).thenAnswer(i -> holder.get() != null && "PENDING".equals(holder.get().getStatus()) ? List.of(holder.get()) : List.of());

        ChatModel chatModel = new FakeChatModel(
                "{\"level\":\"optional\",\"reason\":\"可复用\",\"core_content\":\"用户长期关注Agent记忆优化方案\"}",
                "MUST");
        OpenAiEmbeddingClient embeddingClient = mock(OpenAiEmbeddingClient.class);
        when(embeddingClient.embed(anyString())).thenReturn(List.of(0.1f, 0.2f));
        ElasticsearchUserKnowledgeStore store = mock(ElasticsearchUserKnowledgeStore.class);

        UserKnowledgeService service = new UserKnowledgeService(props, mapper, embeddingClient, store, chatModel);
        MemoryTaskPayloads.QaPayload payload = new MemoryTaskPayloads.QaPayload();
        payload.setQuestion("帮我长期优化 agent 记忆系统");
        payload.setAnswer("可以按摘要驱动、候选态、连续命中确认来做");
        service.classifyAndStore(1L, 2L, "s1", payload);
        assertEquals("PENDING", holder.get().getStatus());
        assertEquals("OPTIONAL", holder.get().getKnowledgeLevel());
        verify(store, never()).upsert(any(), any(), any(), any(), any(), any());

        service.reviewPendingBySummary(1L, 2L, "s1", "用户持续多轮关注Agent记忆优化方案");
        assertEquals("ACTIVE", holder.get().getStatus());
        assertEquals("COMPLETED", holder.get().getIndexStatus());
        verify(store, times(1)).upsert(any(), any(), any(), any(), any(), any());
    }

    @Test
    void duplicateCoreContentShouldNotBeInsertedTwice() {
        LegalMemoryProperties props = new LegalMemoryProperties();
        props.getUserKnowledge().setMinQuestionLength(1);
        UserKnowledgeMapper mapper = mock(UserKnowledgeMapper.class);
        when(mapper.selectCount(any())).thenReturn(1L);
        ChatModel chatModel = new FakeChatModel("{\"level\":\"must\",\"reason\":\"显式记忆\",\"core_content\":\"用户要求长期记住其编码偏好\"}");
        UserKnowledgeService service = new UserKnowledgeService(props, mapper, mock(OpenAiEmbeddingClient.class), mock(ElasticsearchUserKnowledgeStore.class), chatModel);
        MemoryTaskPayloads.QaPayload payload = new MemoryTaskPayloads.QaPayload();
        payload.setQuestion("记住我喜欢详细注释");
        payload.setAnswer("已记录");
        service.classifyAndStore(1L, 2L, "s1", payload);
        verify(mapper, never()).insert(any(UserKnowledgeEntity.class));
    }

    static class FakeChatModel implements ChatModel {
        private final Deque<String> responses = new ArrayDeque<>();
        FakeChatModel(String... responses) { this.responses.addAll(List.of(responses)); }
        @Override public ChatResponse chat(List<ChatMessage> messages) {
            String text = responses.isEmpty() ? "[]" : responses.removeFirst();
            return ChatResponse.builder().aiMessage(AiMessage.from(text)).build();
        }
    }
}
