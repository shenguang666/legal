package com.legal.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.legal.chat.cache.AnswerCachePayload;
import com.legal.chat.cache.AnswerCacheService;
import com.legal.chat.cache.CacheHit;
import com.legal.chat.cache.CacheSafetyGuard;
import com.legal.chat.cache.HighRiskGuard;
import com.legal.chat.cache.KbSnapshotService;
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
import com.legal.enums.ChatMessageRole;
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
import java.util.Optional;

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

    private final AnswerCacheService answerCacheService;
    private final CacheSafetyGuard cacheSafetyGuard;
    private final HighRiskGuard highRiskGuard;
    private final KbSnapshotService kbSnapshotService;

    public ChatService(ChatSessionMapper chatSessionMapper,
                       ChatMessageMapper chatMessageMapper,
                       RetrievalLogMapper retrievalLogMapper,
                       IdempotencyService idempotencyService,
                       RagAnswerService ragAnswerService,
                       StreamingChatModel streamingChatModel,
                       AnswerCacheService answerCacheService,
                       CacheSafetyGuard cacheSafetyGuard,
                       HighRiskGuard highRiskGuard,
                       KbSnapshotService kbSnapshotService) {
        this.chatSessionMapper = chatSessionMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.retrievalLogMapper = retrievalLogMapper;
        this.idempotencyService = idempotencyService;
        this.ragAnswerService = ragAnswerService;
        this.streamingChatModel = streamingChatModel;
        this.answerCacheService = answerCacheService;
        this.cacheSafetyGuard = cacheSafetyGuard;
        this.highRiskGuard = highRiskGuard;
        this.kbSnapshotService = kbSnapshotService;
    }


    private AskResponse toAskResponse(AnswerCachePayload payload) {
        AskResponse resp = new AskResponse();
        resp.setAnswer(payload.getAnswer());
        resp.setCitations(payload.getCitations());
        resp.setConfidence(payload.getConfidence() == null ? 0.0d : payload.getConfidence());
        resp.setWarning(payload.getWarning());
        return resp;
    }

    private AnswerCachePayload toCachePayload(String question, String traceId, RagAnswer ragAnswer) {
        AnswerCachePayload payload = new AnswerCachePayload();
        payload.setAnswer(ragAnswer.getAnswer());
        payload.setCitations(ragAnswer.getCitations());
        payload.setConfidence(ragAnswer.isKnowledgeHit() ? 0.81 : 0.45);
        payload.setWarning("本回答基于知识库检索与大模型生成，仅供法律知识参考，不构成正式法律意见");
        payload.setSourceTraceId(traceId);
        payload.setSourceQuestion(question);
        payload.setModelName(ragAnswer.getModelName());
        payload.setKnowledgeHit(ragAnswer.isKnowledgeHit());
        payload.setHitChunkIds(ragAnswer.getRetrievedChunks().stream()
                .map(chunk -> String.valueOf(chunk.getChunkId()))
                .collect(Collectors.joining(",")));
        return payload;
    }

    private void writeRetrievalLogForCacheHit(AuthPrincipal principal,
                                             String traceId,
                                             String question,
                                             AnswerCachePayload payload,
                                             int latencyMs,
                                             CacheHit hit) {
        RetrievalLogEntity retrievalLog = new RetrievalLogEntity();
        retrievalLog.setTraceId(traceId);
        retrievalLog.setTenantId(principal.tenantId());
        retrievalLog.setQueryText(question);
        retrievalLog.setHitChunkIds(payload.getHitChunkIds());
        retrievalLog.setRerankScore(BigDecimal.ZERO);
        String model = payload.getModelName();
        retrievalLog.setModelName("CACHE-" + (model == null ? "unknown" : model)
                + "-" + hit.getScope() + "-" + hit.getType());
        retrievalLog.setLatencyMs(latencyMs);
        retrievalLog.setCreatedAt(LocalDateTime.now());
        retrievalLogMapper.insert(retrievalLog);
    }

    private void printCacheHit(AuthPrincipal principal, String traceId, CacheHit hit, int latencyMs) {
        if (hit == null) {
            return;
        }
        String score = hit.getSimilarityScore() == null ? "-" : String.format(java.util.Locale.ROOT, "%.4f", hit.getSimilarityScore());
        log.info("CACHE HIT traceId={} tenantId={} userId={} scope={} type={} similarity={} latencyMs={}",
                traceId,
                principal.tenantId(),
                principal.userId(),
                hit.getScope(),
                hit.getType(),
                score,
                latencyMs);
    }

    private void writeRetrievalLogForRag(AuthPrincipal principal,
                                        String traceId,
                                        String question,
                                        RagAnswer ragAnswer,
                                        int latencyMs) {
        RetrievalLogEntity retrievalLog = new RetrievalLogEntity();
        retrievalLog.setTraceId(traceId);
        retrievalLog.setTenantId(principal.tenantId());
        retrievalLog.setQueryText(question);
        retrievalLog.setHitChunkIds(ragAnswer.getRetrievedChunks().stream()
                .map(chunk -> String.valueOf(chunk.getChunkId()))
                .collect(Collectors.joining(",")));
        retrievalLog.setRerankScore(BigDecimal.ZERO);
        retrievalLog.setModelName(ragAnswer.getModelName());
        retrievalLog.setLatencyMs(latencyMs);
        retrievalLog.setCreatedAt(LocalDateTime.now());
        retrievalLogMapper.insert(retrievalLog);
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
	        userMessage.setRole(ChatMessageRole.USER);
        userMessage.setContent(request.getQuestion());
        userMessage.setTokenUsage(0);
        userMessage.setLatencyMs(0);
        userMessage.setTraceId(traceId);
        userMessage.setCreatedAt(LocalDateTime.now());
        chatMessageMapper.insert(userMessage);

        String kbSnapshotVersion = kbSnapshotService.getSnapshotVersion(principal.tenantId());
        String question = request.getQuestion();

        // 高风险问题：绕过缓存读写
        if (!highRiskGuard.isHighRisk(question)) {
            // 1) USER-EXACT
            Optional<CacheHit> hit = answerCacheService.getUserExact(principal.tenantId(), principal.userId(), kbSnapshotVersion, question);
            // 2) TENANT-EXACT
            if (hit.isEmpty() && cacheSafetyGuard.allowTenantCache(question)) {
                hit = answerCacheService.getTenantExact(principal.tenantId(), kbSnapshotVersion, question);
            }
            // 3) USER-SEMANTIC
            if (hit.isEmpty()) {
                hit = answerCacheService.getUserSemantic(principal.tenantId(), principal.userId(), kbSnapshotVersion, question);
            }
            // 4) TENANT-SEMANTIC
            if (hit.isEmpty() && cacheSafetyGuard.allowTenantCache(question)) {
                hit = answerCacheService.getTenantSemantic(principal.tenantId(), kbSnapshotVersion, question);
            }

            if (hit.isPresent()) {
                AnswerCachePayload payload = hit.get().getPayload();

                // 落库 assistant 消息（内容来自缓存）
                ChatMessageEntity assistantMessage = new ChatMessageEntity();
                assistantMessage.setSessionId(session.getSessionId());
	            assistantMessage.setRole(ChatMessageRole.ASSISTANT);
                assistantMessage.setContent(payload.getAnswer());
                assistantMessage.setTokenUsage(0);
                assistantMessage.setTraceId(traceId);
                assistantMessage.setCreatedAt(LocalDateTime.now());
                chatMessageMapper.insert(assistantMessage);

                int latency = (int) (System.currentTimeMillis() - start);
                assistantMessage.setLatencyMs(latency);
                chatMessageMapper.updateById(assistantMessage);

                // 控制台打印缓存命中提示
                printCacheHit(principal, traceId, hit.get(), latency);

                writeRetrievalLogForCacheHit(principal, traceId, question, payload, latency, hit.get());

                session.setLastActiveAt(LocalDateTime.now());
                chatSessionMapper.updateById(session);

                // 按前端约定推送
                try {
                    emitter.send(SseEmitter.event().name("answer").data(new AskStreamEvent("answer", payload.getAnswer())));
                    emitter.send(SseEmitter.event().name("citations").data(new AskStreamEvent("citations", JsonUtils.toJson(payload.getCitations()))));
                    emitter.send(SseEmitter.event().name("done").data(new AskStreamEvent("done", "")));
                    emitter.complete();
                } catch (Exception ex) {
                    emitter.completeWithError(ex);
                }
                return emitter;
            }
        }

        // 未命中缓存：继续走现有流式流程
        RagAnswer ragAnswer = ragAnswerService.retrieveOnly(principal.tenantId(), session.getSessionId(), question);
        // 复用已检索的 chunks 构建 systemPrompt，避免一次请求内重复检索导致重复日志
        String systemPrompt = ragAnswerService.buildSystemPromptForStreaming(question, ragAnswer.getRetrievedChunks());

        ChatMemory chatMemory = ragAnswerService.createMemoryForStreaming(session.getSessionId());
        List<ChatMessage> messages = chatMemory.messages();
        messages.add(SystemMessage.from(systemPrompt));
        messages.add(UserMessage.from(question));

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
	        assistantMessage.setRole(ChatMessageRole.ASSISTANT);
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
                retrievalLog.setQueryText(question);
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

                // 仅非高风险才写缓存
                if (!highRiskGuard.isHighRisk(question)) {
                    AnswerCachePayload payload = new AnswerCachePayload();
                    payload.setAnswer(answerBuf.toString());
                    payload.setCitations(ragAnswer.getCitations());
                    payload.setConfidence(ragAnswer.isKnowledgeHit() ? 0.81 : 0.45);
                    payload.setWarning("本回答基于知识库检索与大模型生成，仅供法律知识参考，不构成正式法律意见");
                    payload.setSourceTraceId(traceId);
                    payload.setSourceQuestion(question);
                    payload.setModelName(ragAnswer.getModelName());
                    payload.setKnowledgeHit(ragAnswer.isKnowledgeHit());
                    payload.setHitChunkIds(ragAnswer.getRetrievedChunks().stream()
                            .map(chunk -> String.valueOf(chunk.getChunkId()))
                            .collect(Collectors.joining(",")));

                    answerCacheService.putUserExact(principal.tenantId(), principal.userId(), kbSnapshotVersion, question, payload);
                    answerCacheService.putUserSemanticCandidate(principal.tenantId(), principal.userId(), kbSnapshotVersion, question, payload);
                    if (cacheSafetyGuard.allowTenantCache(question)) {
                        answerCacheService.putTenantExact(principal.tenantId(), kbSnapshotVersion, question, payload);
                        answerCacheService.putTenantSemanticCandidate(principal.tenantId(), kbSnapshotVersion, question, payload);
                    }
                }

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

        String question = request.getQuestion();

        ChatMessageEntity userMessage = new ChatMessageEntity();
        userMessage.setSessionId(session.getSessionId());
	        userMessage.setRole(ChatMessageRole.USER);
        userMessage.setContent(question);
        userMessage.setTokenUsage(0);
        userMessage.setLatencyMs(0);
        userMessage.setTraceId(traceId);
        userMessage.setCreatedAt(LocalDateTime.now());
        chatMessageMapper.insert(userMessage);

        String kbSnapshotVersion = kbSnapshotService.getSnapshotVersion(principal.tenantId());

        // 高风险问题：绕过缓存读写
        if (!highRiskGuard.isHighRisk(question)) {
            // 1) USER-EXACT
            Optional<CacheHit> hit = answerCacheService.getUserExact(principal.tenantId(), principal.userId(), kbSnapshotVersion, question);
            // 2) TENANT-EXACT
            if (hit.isEmpty() && cacheSafetyGuard.allowTenantCache(question)) {
                hit = answerCacheService.getTenantExact(principal.tenantId(), kbSnapshotVersion, question);
            }
            // 3) USER-SEMANTIC
            if (hit.isEmpty()) {
                hit = answerCacheService.getUserSemantic(principal.tenantId(), principal.userId(), kbSnapshotVersion, question);
            }
            // 4) TENANT-SEMANTIC
            if (hit.isEmpty() && cacheSafetyGuard.allowTenantCache(question)) {
                hit = answerCacheService.getTenantSemantic(principal.tenantId(), kbSnapshotVersion, question);
            }

            if (hit.isPresent()) {
                AnswerCachePayload payload = hit.get().getPayload();

                ChatMessageEntity assistantMessage = new ChatMessageEntity();
                assistantMessage.setSessionId(session.getSessionId());
	                assistantMessage.setRole(ChatMessageRole.ASSISTANT);
                assistantMessage.setContent(payload.getAnswer());
                assistantMessage.setTokenUsage(0);
                assistantMessage.setTraceId(traceId);
                assistantMessage.setCreatedAt(LocalDateTime.now());
                chatMessageMapper.insert(assistantMessage);

                int latency = (int) (System.currentTimeMillis() - start);
                assistantMessage.setLatencyMs(latency);
                chatMessageMapper.updateById(assistantMessage);

                writeRetrievalLogForCacheHit(principal, traceId, question, payload, latency, hit.get());

                session.setLastActiveAt(LocalDateTime.now());
                chatSessionMapper.updateById(session);

                return toAskResponse(payload);
            }
        }

        RagAnswer ragAnswer = ragAnswerService.answer(principal.tenantId(), session.getSessionId(), question);

        ChatMessageEntity assistantMessage = new ChatMessageEntity();
        assistantMessage.setSessionId(session.getSessionId());
	        assistantMessage.setRole(ChatMessageRole.ASSISTANT);
        assistantMessage.setContent(ragAnswer.getAnswer());
        assistantMessage.setTokenUsage(ragAnswer.getTokenUsage());
        assistantMessage.setTraceId(traceId);
        assistantMessage.setCreatedAt(LocalDateTime.now());
        chatMessageMapper.insert(assistantMessage);

        int latency = (int) (System.currentTimeMillis() - start);
        assistantMessage.setLatencyMs(latency);
        chatMessageMapper.updateById(assistantMessage);

        writeRetrievalLogForRag(principal, traceId, question, ragAnswer, latency);

        session.setLastActiveAt(LocalDateTime.now());
        chatSessionMapper.updateById(session);

        // 非高风险：写入缓存（用户级必写；租户级需安全判定）
        if (!highRiskGuard.isHighRisk(question)) {
            AnswerCachePayload payload = toCachePayload(question, traceId, ragAnswer);
            answerCacheService.putUserExact(principal.tenantId(), principal.userId(), kbSnapshotVersion, question, payload);
            answerCacheService.putUserSemanticCandidate(principal.tenantId(), principal.userId(), kbSnapshotVersion, question, payload);
            if (cacheSafetyGuard.allowTenantCache(question)) {
                answerCacheService.putTenantExact(principal.tenantId(), kbSnapshotVersion, question, payload);
                answerCacheService.putTenantSemanticCandidate(principal.tenantId(), kbSnapshotVersion, question, payload);
            }
        }

        AskResponse response = new AskResponse();
        response.setAnswer(ragAnswer.getAnswer());
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
        dto.setRole(entity.getRole() == null ? null : entity.getRole().getCode());
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
