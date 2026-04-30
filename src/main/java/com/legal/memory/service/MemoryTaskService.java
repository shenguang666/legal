package com.legal.memory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.legal.chat.entity.ChatMessageEntity;
import com.legal.chat.mapper.ChatMessageMapper;
import com.legal.common.JsonUtils;
import com.legal.config.LegalMemoryProperties;
import com.legal.enums.ChatMessageRole;
import com.legal.enums.MemoryTaskStatus;
import com.legal.enums.MemoryTaskType;
import com.legal.memory.entity.MemoryTaskOutboxEntity;
import com.legal.memory.mapper.MemoryTaskOutboxMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 记忆任务入队（仅做 outbox 写入，不执行重逻辑）。
 */
@Service
public class MemoryTaskService {

    private final LegalMemoryProperties props;
    private final ChatMessageMapper chatMessageMapper;
    private final MemoryTaskOutboxMapper memoryTaskOutboxMapper;

    public MemoryTaskService(LegalMemoryProperties props,
                             ChatMessageMapper chatMessageMapper,
                             MemoryTaskOutboxMapper memoryTaskOutboxMapper) {
        this.props = props;
        this.chatMessageMapper = chatMessageMapper;
        this.memoryTaskOutboxMapper = memoryTaskOutboxMapper;
    }

    /** assistant 消息落库后调用：达到 refreshRounds 时只触发摘要任务。长期记忆将在摘要完成后再识别。 */
    public void enqueueSummaryIfNeeded(Long tenantId, Long userId, String sessionId) {
        if (!props.isEnabled() || !props.getSummary().isEnabled()) return;
        int refreshRounds = Math.max(1, props.getSummary().getRefreshRounds());
        Long assistantCount = chatMessageMapper.selectCount(
                new LambdaQueryWrapper<ChatMessageEntity>()
                        .eq(ChatMessageEntity::getSessionId, sessionId)
                        .eq(ChatMessageEntity::getRole, ChatMessageRole.ASSISTANT));
        if (assistantCount == null || assistantCount <= 0 || assistantCount % refreshRounds != 0) return;
        MemoryTaskPayloads.SummaryPayload payload = new MemoryTaskPayloads.SummaryPayload();
        payload.setAssistantCount(assistantCount);
        insertTask(tenantId, userId, sessionId, MemoryTaskType.SUMMARY_REFRESH, JsonUtils.toJson(payload));
    }

    /** 每轮问答完成后自动入队：仅用户外挂知识索引。长期记忆不逐轮抽取，避免草率和性能损耗。 */
    public void enqueuePostAskTasks(Long tenantId, Long userId, String sessionId,
                                    Long userMessageId, Long assistantMessageId,
                                    String question, String answer) {
        if (!props.getUserKnowledge().isEnabled()) return;
        MemoryTaskPayloads.QaPayload payload = new MemoryTaskPayloads.QaPayload();
        payload.setUserMessageId(userMessageId);
        payload.setAssistantMessageId(assistantMessageId);
        payload.setQuestion(question);
        payload.setAnswer(answer);
        insertTask(tenantId, userId, sessionId, MemoryTaskType.USER_KNOWLEDGE_INDEX, JsonUtils.toJson(payload));
    }

    private void insertTask(Long tenantId, Long userId, String sessionId, MemoryTaskType type, String payload) {
        MemoryTaskOutboxEntity outbox = new MemoryTaskOutboxEntity();
        outbox.setTenantId(tenantId);
        outbox.setUserId(userId);
        outbox.setSessionId(sessionId);
        outbox.setTaskType(type);
        outbox.setPayload(payload);
        outbox.setStatus(MemoryTaskStatus.PENDING);
        outbox.setRetryCount(0);
        outbox.setCreatedAt(LocalDateTime.now());
        outbox.setUpdatedAt(LocalDateTime.now());
        memoryTaskOutboxMapper.insert(outbox);
    }
}
