package com.legal.memory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.legal.chat.entity.ChatMessageEntity;
import com.legal.chat.mapper.ChatMessageMapper;
import com.legal.config.LegalMemoryProperties;
import com.legal.enums.ChatMessageRole;
import com.legal.memory.entity.ChatMemorySummaryEntity;
import com.legal.memory.mapper.ChatMemorySummaryMapper;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 摘要生成服务（由 outbox worker 调用）。
 */
@Service
public class MemorySummaryService {

    private final LegalMemoryProperties props;
    private final ChatMessageMapper chatMessageMapper;
    private final ChatMemorySummaryMapper chatMemorySummaryMapper;
    private final ChatModel chatModel;

    public MemorySummaryService(LegalMemoryProperties props,
                                ChatMessageMapper chatMessageMapper,
                                ChatMemorySummaryMapper chatMemorySummaryMapper,
                                @Nullable ChatModel chatModel) {
        this.props = props;
        this.chatMessageMapper = chatMessageMapper;
        this.chatMemorySummaryMapper = chatMemorySummaryMapper;
        this.chatModel = chatModel;
    }

    public void refreshSummary(Long tenantId, Long userId, String sessionId) {
        if (!props.isEnabled() || !props.getSummary().isEnabled()) {
            return;
        }
        if (chatModel == null) {
            // 无 LLM 配置时，跳过摘要生成
            return;
        }

        // assistant 轮次（一次成功ask=assistant消息落库一次）
        long assistantCount = chatMessageMapper.selectCount(
                new LambdaQueryWrapper<ChatMessageEntity>()
                        .eq(ChatMessageEntity::getSessionId, sessionId)
                        .eq(ChatMessageEntity::getRole, ChatMessageRole.ASSISTANT)
        );

        // 取现有摘要
        ChatMemorySummaryEntity summary = chatMemorySummaryMapper.selectOne(
                new LambdaQueryWrapper<ChatMemorySummaryEntity>()
                        .eq(ChatMemorySummaryEntity::getTenantId, tenantId)
                        .eq(ChatMemorySummaryEntity::getUserId, userId)
                        .eq(ChatMemorySummaryEntity::getSessionId, sessionId)
                        .last("limit 1")
        );

        int existingRound = summary == null || summary.getRoundCount() == null ? 0 : summary.getRoundCount();
        if (assistantCount <= existingRound) {
            return;
        }

        // 取最近 N 条消息
        int lastMessages = Math.max(5, props.getSummary().getLastMessages());
        List<ChatMessageEntity> recent = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessageEntity>()
                        .eq(ChatMessageEntity::getSessionId, sessionId)
                        .orderByDesc(ChatMessageEntity::getCreatedAt)
                        .last("limit " + lastMessages)
        );
        Collections.reverse(recent);

        String old = summary == null ? "" : (summary.getSummaryText() == null ? "" : summary.getSummaryText());
        String transcript = buildTranscript(recent);

        String prompt = "你是对话摘要器。请将对话压缩为结构化摘要(JSON)。要求：\n"
                + "1) 保留已确认事实、用户目标、未解决问题、关键偏好。\n"
                + "2) 不要编造信息；不确定写unknown。\n"
                + "3) 输出严格JSON，不要多余文本。\n\n"
                + "旧摘要：\n" + old + "\n\n"
                + "新增对话：\n" + transcript;

        String result = chatModel.chat(List.of(
                SystemMessage.from("你是严谨的JSON摘要生成器"),
                UserMessage.from(prompt)
        )).aiMessage().text();

        if (!StringUtils.hasText(result)) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        if (summary == null) {
            summary = new ChatMemorySummaryEntity();
            summary.setTenantId(tenantId);
            summary.setUserId(userId);
            summary.setSessionId(sessionId);
            summary.setVersion(1);
            summary.setCreatedAt(now);
        }
        summary.setSummaryText(result);
        summary.setRoundCount((int) assistantCount);
        summary.setUpdatedAt(now);

        if (summary.getId() == null) {
            chatMemorySummaryMapper.insert(summary);
        } else {
            chatMemorySummaryMapper.updateById(summary);
        }
    }

    public String findSummaryText(Long tenantId, Long userId, String sessionId) {
        ChatMemorySummaryEntity summary = chatMemorySummaryMapper.selectOne(
                new LambdaQueryWrapper<ChatMemorySummaryEntity>()
                        .eq(ChatMemorySummaryEntity::getTenantId, tenantId)
                        .eq(ChatMemorySummaryEntity::getUserId, userId)
                        .eq(ChatMemorySummaryEntity::getSessionId, sessionId)
                        .last("limit 1")
        );
        return summary == null ? "" : summary.getSummaryText();
    }


    private String buildTranscript(List<ChatMessageEntity> messages) {
        StringBuilder sb = new StringBuilder();
        for (ChatMessageEntity m : messages) {
            String role = m.getRole() == ChatMessageRole.ASSISTANT ? "assistant" : "user";
            sb.append(role).append(": ").append(m.getContent()).append("\n");
        }
        return sb.toString();
    }
}
