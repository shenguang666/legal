package com.legal.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.legal.chat.dto.AskRequest;
import com.legal.chat.dto.AskResponse;
import com.legal.chat.dto.AskStreamEvent;
import com.legal.chat.dto.ChatMessageDto;
import com.legal.chat.dto.ChatSessionDto;
import com.legal.chat.dto.CreateSessionRequest;
import com.legal.chat.dto.CreateSessionResponse;
import com.legal.chat.entity.ChatMessageEntity;
import com.legal.chat.entity.ChatSessionEntity;
import com.legal.chat.mapper.ChatMessageMapper;
import com.legal.chat.mapper.ChatSessionMapper;
import com.legal.chat.rag.RagAnswer;
import com.legal.chat.rag.RagAnswerService;
import com.legal.common.AppException;
import com.legal.common.JsonUtils;
import com.legal.retrieval.entity.RetrievalLogEntity;
import com.legal.retrieval.mapper.RetrievalLogMapper;
import com.legal.security.AuthPrincipal;
import com.legal.security.IdempotencyService;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.PartialThinking;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final RetrievalLogMapper retrievalLogMapper;
    private final IdempotencyService idempotencyService;
    private final RagAnswerService ragAnswerService;
    private final StreamingChatModel streamingChatModel;

    public ChatService(ChatSessionMapper chatSessionMapper,
                       ChatMessageMapper chatMessageMapper,
                       RetrievalLogMapper retrievalLogMapper,
                       IdempotencyService idempotencyService,
                       RagAnswerService ragAnswerService,
                       StreamingChatModel streamingChatModel) {
        this.chatSessionMapper = chatSessionMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.retrievalLogMapper = retrievalLogMapper;
        this.idempotencyService = idempotencyService;
        this.ragAnswerService = ragAnswerService;
        this.streamingChatModel = streamingChatModel;
    }

    /**
     * 新增：SSE 流式问答。
     * - 旧的 ask(...) 保留
     * - thinking/answer 分事件推送给前端
     */
    public SseEmitter askStream(AuthPrincipal principal, AskRequest request, String traceId) {
        if (streamingChatModel == null) {
            throw AppException.badRequest("请先配置 LEGAL_LLM_API_KEY 后再使用流式问答");
        }

        // 流式连接可能较久，超时设长一点
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);
        long start = System.currentTimeMillis();

        // 仍保持幂等语义：开始即占用 requestId
        idempotencyService.ensureUnique(principal, "chat:ask:stream", request.getRequestId());
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

        // 先做 RAG 检索（内部会打印检索日志与 2B 指标），构建 systemPrompt（不调用同步模型）
        RagAnswer ragAnswer = ragAnswerService.retrieveOnly(principal.tenantId(), session.getSessionId(), request.getQuestion());
        String systemPrompt = ragAnswerService.buildSystemPromptForStreaming(principal.tenantId(), session.getSessionId(), request.getQuestion());

        ChatMemory chatMemory = ragAnswerService.createMemoryForStreaming(session.getSessionId());
        List<ChatMessage> messages = chatMemory.messages();
        messages.add(SystemMessage.from(systemPrompt));
        messages.add(UserMessage.from(request.getQuestion()));

        StringBuilder thinkingBuf = new StringBuilder();
        StringBuilder answerBuf = new StringBuilder();

        // 异步执行，避免占用请求线程
        CompletableFuture.runAsync(() -> streamingChatModel.chat(messages, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                // 默认都作为 answer 输出；如果模型原生支持 thinking 事件，会走 onPartialThinking
                answerBuf.append(partialResponse);
                send("answer", partialResponse);
            }

            @Override
            public void onPartialThinking(PartialThinking partialThinking) {
                String t = partialThinking == null ? "" : String.valueOf(partialThinking.text());
                if (StringUtils.hasText(t)) {
                    thinkingBuf.append(t);
                    send("thinking", t);
                }
            }

            @Override
            public void onCompleteResponse(dev.langchain4j.model.chat.response.ChatResponse response) {
                // 落库 assistant 消息
                ChatMessageEntity assistantMessage = new ChatMessageEntity();
                assistantMessage.setSessionId(session.getSessionId());
                assistantMessage.setRole("assistant");
                assistantMessage.setContent(answerBuf.toString());
                assistantMessage.setTokenUsage(response.tokenUsage() == null ? 0 : response.tokenUsage().totalTokenCount());
                assistantMessage.setTraceId(traceId);
                assistantMessage.setCreatedAt(LocalDateTime.now());
                chatMessageMapper.insert(assistantMessage);

                int latency = (int) (System.currentTimeMillis() - start);
                assistantMessage.setLatencyMs(latency);
                chatMessageMapper.updateById(assistantMessage);

                // retrieval log
                RetrievalLogEntity retrievalLog = new RetrievalLogEntity();
                retrievalLog.setTraceId(traceId);
                retrievalLog.setTenantId(principal.tenantId());
                retrievalLog.setQueryText(request.getQuestion());
                retrievalLog.setHitChunkIds(ragAnswer.getRetrievedChunks().stream()
                        .map(chunk -> String.valueOf(chunk.getChunkId()))
                        .collect(Collectors.joining(",")));
                retrievalLog.setRerankScore(BigDecimal.ZERO);
                retrievalLog.setModelName(ragAnswer.getModelName());
                retrievalLog.setLatencyMs(latency);
                retrievalLog.setCreatedAt(LocalDateTime.now());
                retrievalLogMapper.insert(retrievalLog);

                session.setLastActiveAt(LocalDateTime.now());
                chatSessionMapper.updateById(session);

                // citations 一次性发给前端
                send("citations", JsonUtils.toJson(ragAnswer.getCitations()));
                send("done", "");
                emitter.complete();
            }

            @Override
            public void onError(Throwable error) {
                log.error("streaming ask failed traceId={}", traceId, error);
                try {
                    send("error", error.getMessage() == null ? "stream error" : error.getMessage());
                } finally {
                    emitter.completeWithError(error);
                }
            }

            private void send(String type, String data) {
                try {
                    emitter.send(SseEmitter.event()
                            .name(type)
                            .data(new AskStreamEvent(type, data)));
                } catch (Exception ex) {
                    emitter.completeWithError(ex);
                }
            }
        }));

        return emitter;
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

        RagAnswer ragAnswer = ragAnswerService.answer(principal.tenantId(), session.getSessionId(), request.getQuestion());
        String answer = ragAnswer.getAnswer();

        ChatMessageEntity assistantMessage = new ChatMessageEntity();
        assistantMessage.setSessionId(session.getSessionId());
        assistantMessage.setRole("assistant");
        assistantMessage.setContent(answer);
        assistantMessage.setTokenUsage(ragAnswer.getTokenUsage());
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
        retrievalLog.setHitChunkIds(ragAnswer.getRetrievedChunks().stream()
                .map(chunk -> String.valueOf(chunk.getChunkId()))
                .collect(Collectors.joining(",")));
        retrievalLog.setRerankScore(BigDecimal.ZERO);
        retrievalLog.setModelName(ragAnswer.getModelName());
        retrievalLog.setLatencyMs(latency);
        retrievalLog.setCreatedAt(LocalDateTime.now());
        retrievalLogMapper.insert(retrievalLog);

        session.setLastActiveAt(LocalDateTime.now());
        chatSessionMapper.updateById(session);

        AskResponse response = new AskResponse();
        response.setAnswer(answer);
        response.setCitations(ragAnswer.getCitations());
        response.setConfidence(ragAnswer.isKnowledgeHit() ? 0.81 : 0.45);
        response.setWarning("本回答基于知识库检索与大模型生成，仅供法律知识参考，不构成正式法律意见");
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
