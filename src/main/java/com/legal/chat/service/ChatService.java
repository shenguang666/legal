package com.legal.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.legal.chat.dto.AskRequest;
import com.legal.chat.dto.AskResponse;
import com.legal.chat.dto.ChatMessageDto;
import com.legal.chat.dto.ChatSessionDto;
import com.legal.chat.dto.CitationDto;
import com.legal.chat.dto.CreateSessionRequest;
import com.legal.chat.dto.CreateSessionResponse;
import com.legal.chat.entity.ChatMessageEntity;
import com.legal.chat.entity.ChatSessionEntity;
import com.legal.chat.mapper.ChatMessageMapper;
import com.legal.chat.mapper.ChatSessionMapper;
import com.legal.common.AppException;
import com.legal.retrieval.entity.RetrievalLogEntity;
import com.legal.retrieval.mapper.RetrievalLogMapper;
import com.legal.security.AuthPrincipal;
import com.legal.security.IdempotencyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ChatService {

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final RetrievalLogMapper retrievalLogMapper;
    private final IdempotencyService idempotencyService;

    public ChatService(ChatSessionMapper chatSessionMapper,
                       ChatMessageMapper chatMessageMapper,
                       RetrievalLogMapper retrievalLogMapper,
                       IdempotencyService idempotencyService) {
        this.chatSessionMapper = chatSessionMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.retrievalLogMapper = retrievalLogMapper;
        this.idempotencyService = idempotencyService;
    }

    @Transactional
    public CreateSessionResponse createSession(AuthPrincipal principal, CreateSessionRequest request) {
        idempotencyService.ensureUnique(principal, "chat:create-session", request.getRequestId());
        String title = StringUtils.hasText(request.getTitle()) ? request.getTitle() : "新会话";

        ChatSessionEntity entity = new ChatSessionEntity();
        entity.setSessionId("s_" + UUID.randomUUID().toString().replace("-", ""));
        entity.setTenantId(principal.tenantId());
        entity.setOwnerUserId(principal.userId());
        entity.setTitle(title);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setLastActiveAt(LocalDateTime.now());
        chatSessionMapper.insert(entity);

        CreateSessionResponse response = new CreateSessionResponse();
        response.setSessionId(entity.getSessionId());
        response.setTitle(entity.getTitle());
        response.setCreatedAt(entity.getCreatedAt());
        return response;
    }

    public List<ChatMessageDto> listMessages(AuthPrincipal principal, String sessionId) {
        requireSession(principal, sessionId);
        return chatMessageMapper.selectList(
                        new LambdaQueryWrapper<ChatMessageEntity>()
                                .eq(ChatMessageEntity::getSessionId, sessionId)
                                .orderByAsc(ChatMessageEntity::getCreatedAt)
                ).stream()
                .map(this::toMessageDto)
                .toList();
    }

    public List<ChatSessionDto> listSessions(AuthPrincipal principal) {
        return chatSessionMapper.selectList(
                        new LambdaQueryWrapper<ChatSessionEntity>()
                                .eq(ChatSessionEntity::getTenantId, principal.tenantId())
                                .eq(ChatSessionEntity::getOwnerUserId, principal.userId())
                                .orderByDesc(ChatSessionEntity::getLastActiveAt)
                ).stream()
                .map(this::toSessionDto)
                .toList();
    }

    @Transactional
    public AskResponse ask(AuthPrincipal principal, AskRequest request, String traceId) {
        long start = System.currentTimeMillis();
        idempotencyService.ensureUnique(principal, "chat:ask", request.getRequestId());
        ChatSessionEntity session = requireSession(principal, request.getSessionId());

        ChatMessageEntity userMessage = new ChatMessageEntity();
        userMessage.setSessionId(session.getSessionId());
        userMessage.setRole("user");
        userMessage.setContent(request.getQuestion());
        userMessage.setTokenUsage(0);
        userMessage.setLatencyMs(0);
        userMessage.setTraceId(traceId);
        userMessage.setCreatedAt(LocalDateTime.now());
        chatMessageMapper.insert(userMessage);

        String answer = "基础版本回复：已收到你的问题“" + request.getQuestion() + "”。"
                + "当前环境已打通会话、审计与接口链路，下一步可接入向量检索与大模型生成。"
                + "【免责声明】本系统仅提供法律知识参考，不构成法律意见。";

        ChatMessageEntity assistantMessage = new ChatMessageEntity();
        assistantMessage.setSessionId(session.getSessionId());
        assistantMessage.setRole("assistant");
        assistantMessage.setContent(answer);
        assistantMessage.setTokenUsage(0);
        assistantMessage.setTraceId(traceId);
        assistantMessage.setCreatedAt(LocalDateTime.now());
        chatMessageMapper.insert(assistantMessage);

        int latency = (int) (System.currentTimeMillis() - start);
        assistantMessage.setLatencyMs(latency);
        chatMessageMapper.updateById(assistantMessage);

        RetrievalLogEntity retrievalLog = new RetrievalLogEntity();
        retrievalLog.setTraceId(traceId);
        retrievalLog.setTenantId(principal.tenantId());
        retrievalLog.setQueryText(request.getQuestion());
        retrievalLog.setHitChunkIds(null);
        retrievalLog.setRerankScore(BigDecimal.ZERO);
        retrievalLog.setModelName("baseline-mock");
        retrievalLog.setLatencyMs(latency);
        retrievalLog.setCreatedAt(LocalDateTime.now());
        retrievalLogMapper.insert(retrievalLog);

        session.setLastActiveAt(LocalDateTime.now());
        chatSessionMapper.updateById(session);

        AskResponse response = new AskResponse();
        response.setAnswer(answer);
        response.setCitations(List.of(
                new CitationDto(0L, "基础知识库（占位）", "当前基础版本尚未接入真实检索片段")
        ));
        response.setConfidence(0.42);
        response.setWarning("该回答来自基础版本占位逻辑，请勿直接用于法律决策");
        return response;
    }

    private ChatSessionEntity requireSession(AuthPrincipal principal, String sessionId) {
        ChatSessionEntity session = chatSessionMapper.selectOne(
                new LambdaQueryWrapper<ChatSessionEntity>()
                        .eq(ChatSessionEntity::getSessionId, sessionId)
                        .eq(ChatSessionEntity::getTenantId, principal.tenantId())
                        .eq(ChatSessionEntity::getOwnerUserId, principal.userId())
                        .last("limit 1")
        );
        if (session == null) {
            throw AppException.notFound("会话不存在");
        }
        return session;
    }

    private ChatMessageDto toMessageDto(ChatMessageEntity entity) {
        ChatMessageDto dto = new ChatMessageDto();
        dto.setMessageId(entity.getMessageId());
        dto.setRole(entity.getRole());
        dto.setContent(entity.getContent());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    private ChatSessionDto toSessionDto(ChatSessionEntity entity) {
        ChatSessionDto dto = new ChatSessionDto();
        dto.setSessionId(entity.getSessionId());
        dto.setTitle(entity.getTitle());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setLastActiveAt(entity.getLastActiveAt());
        return dto;
    }
}
