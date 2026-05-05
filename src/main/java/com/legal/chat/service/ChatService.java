package com.legal.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.legal.chat.cache.AnswerCachePayload;
import com.legal.chat.cache.AnswerCacheService;
import com.legal.chat.cache.CacheHit;
import com.legal.chat.cache.CacheSafetyGuard;
import com.legal.chat.cache.HighRiskGuard;
import com.legal.chat.cache.KbSnapshotService;
import com.legal.chat.cache.ChatMessageCacheService;
import com.legal.config.LegalMemoryProperties;
import com.legal.memory.service.MemoryContextService;
import com.legal.memory.service.MemoryTaskService;

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
import com.legal.chat.rag.RetrievedChunk;
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
import java.net.SocketException;
import java.util.Optional;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
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


    private final LegalMemoryProperties legalMemoryProperties;
    private final ChatMessageCacheService chatMessageCacheService;
    private final MemoryContextService memoryContextService;
    private final MemoryTaskService memoryTaskService;

    private final AnswerCacheService answerCacheService;
    private final CacheSafetyGuard cacheSafetyGuard;
    private final HighRiskGuard highRiskGuard;
    private final KbSnapshotService kbSnapshotService;
    private final HotwordService hotwordService;

    public ChatService(ChatSessionMapper chatSessionMapper,
                       ChatMessageMapper chatMessageMapper,
                       RetrievalLogMapper retrievalLogMapper,
                       IdempotencyService idempotencyService,
                       RagAnswerService ragAnswerService,
                       StreamingChatModel streamingChatModel,
                       LegalMemoryProperties legalMemoryProperties,
                       ChatMessageCacheService chatMessageCacheService,
                       MemoryContextService memoryContextService,
                       MemoryTaskService memoryTaskService,
                       AnswerCacheService answerCacheService,
                       CacheSafetyGuard cacheSafetyGuard,
                       HighRiskGuard highRiskGuard,
                       KbSnapshotService kbSnapshotService,
                       HotwordService hotwordService) {
        this.chatSessionMapper = chatSessionMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.retrievalLogMapper = retrievalLogMapper;
        this.idempotencyService = idempotencyService;
        this.ragAnswerService = ragAnswerService;
        this.streamingChatModel = streamingChatModel;

        this.legalMemoryProperties = legalMemoryProperties;
        this.chatMessageCacheService = chatMessageCacheService;
        this.memoryContextService = memoryContextService;
        this.memoryTaskService = memoryTaskService;

        this.answerCacheService = answerCacheService;
        this.cacheSafetyGuard = cacheSafetyGuard;
        this.highRiskGuard = highRiskGuard;
        this.kbSnapshotService = kbSnapshotService;
        this.hotwordService = hotwordService;
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
        payload.setHitChunkIds(joinChunkIds(ragAnswer.getRetrievedChunks()));
        payload.setRawHitChunkIds(joinChunkIds(ragAnswer.getRawRetrievedChunks()));
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
        retrievalLog.setRawHitChunkIds(StringUtils.hasText(payload.getRawHitChunkIds()) ? payload.getRawHitChunkIds() : payload.getHitChunkIds());
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
        retrievalLog.setHitChunkIds(joinChunkIds(ragAnswer.getRetrievedChunks()));
        retrievalLog.setRawHitChunkIds(joinChunkIds(ragAnswer.getRawRetrievedChunks()));
        retrievalLog.setRerankScore(BigDecimal.ZERO);
        retrievalLog.setModelName(ragAnswer.getModelName());
        retrievalLog.setLatencyMs(latencyMs);
        retrievalLog.setCreatedAt(LocalDateTime.now());
        retrievalLogMapper.insert(retrievalLog);
    }

    private String joinChunkIds(List<RetrievedChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return "";
        }
        return chunks.stream()
                .map(chunk -> String.valueOf(chunk.getChunkId()))
                .collect(Collectors.joining(","));
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
        AtomicBoolean streamClosed = new AtomicBoolean(false);
        emitter.onCompletion(() -> streamClosed.set(true));
        emitter.onTimeout(() -> streamClosed.set(true));
        emitter.onError(error -> {
            streamClosed.set(true);
            if (isClientDisconnect(error)) {
                log.debug("streaming ask client disconnected traceId={}", traceId);
            }
        });

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
        // 写入消息缓存（若缓存已建立）
        java.time.Duration msgTtl = java.time.Duration.ofDays(Math.max(1, legalMemoryProperties.getCache().getMessagesTtlDays()));
        chatMessageCacheService.appendIfPresent(principal.tenantId(), principal.userId(), session.getSessionId(), userMessage, msgTtl);

        String kbSnapshotVersion = kbSnapshotService.getSnapshotVersion(principal.tenantId());
        String question = request.getQuestion();

        Optional<String> hotwordAnswer = hotwordService.findPresetAnswer(principal.tenantId(), request.getHotwordKey());
        if (hotwordAnswer.isPresent()) {
            String answer = hotwordAnswer.get();
            ChatMessageEntity assistantMessage = new ChatMessageEntity();
            assistantMessage.setSessionId(session.getSessionId());
            assistantMessage.setRole(ChatMessageRole.ASSISTANT);
            assistantMessage.setContent(answer);
            assistantMessage.setTokenUsage(0);
            assistantMessage.setTraceId(traceId);
            assistantMessage.setCreatedAt(LocalDateTime.now());
            chatMessageMapper.insert(assistantMessage);

            int latency = (int) (System.currentTimeMillis() - start);
            assistantMessage.setLatencyMs(latency);
            chatMessageMapper.updateById(assistantMessage);
            chatMessageCacheService.appendIfPresent(principal.tenantId(), principal.userId(), session.getSessionId(), assistantMessage, msgTtl);
            memoryTaskService.enqueueSummaryIfNeeded(principal.tenantId(), principal.userId(), session.getSessionId());
            memoryTaskService.enqueuePostAskTasks(
                    principal.tenantId(),
                    principal.userId(),
                    session.getSessionId(),
                    userMessage.getMessageId(),
                    assistantMessage.getMessageId(),
                    question,
                    answer
            );
            session.setLastActiveAt(LocalDateTime.now());
            chatSessionMapper.updateById(session);

            try {
                safeSend(emitter, streamClosed, traceId, "answer", answer);
                safeSend(emitter, streamClosed, traceId, "citations", "[]");
                safeSend(emitter, streamClosed, traceId, "done", "");
            } catch (Exception ex) {
                log.warn("streaming hotword response failed traceId={}", traceId, ex);
            } finally {
                safeComplete(emitter, streamClosed);
            }
            return emitter;
        }

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
                // 写入消息缓存（若缓存已建立）
                chatMessageCacheService.appendIfPresent(principal.tenantId(), principal.userId(), session.getSessionId(), assistantMessage, msgTtl);

                int latency = (int) (System.currentTimeMillis() - start);
                assistantMessage.setLatencyMs(latency);
                chatMessageMapper.updateById(assistantMessage);

                // 控制台打印缓存命中提示
                printCacheHit(principal, traceId, hit.get(), latency);

                writeRetrievalLogForCacheHit(principal, traceId, question, payload, latency, hit.get());

                session.setLastActiveAt(LocalDateTime.now());
                chatSessionMapper.updateById(session);

                // 按前端约定推送

                // 摘要任务入队（异步，不阻塞主流程）
                memoryTaskService.enqueueSummaryIfNeeded(principal.tenantId(), principal.userId(), session.getSessionId());
                // 自动入队：长期记忆抽取 + 用户外挂知识索引
                memoryTaskService.enqueuePostAskTasks(
                        principal.tenantId(),
                        principal.userId(),
                        session.getSessionId(),
                        userMessage.getMessageId(),
                        assistantMessage.getMessageId(),
                        question,
                        payload.getAnswer()
                );


                try {
                    safeSend(emitter, streamClosed, traceId, "answer", payload.getAnswer());
                    safeSend(emitter, streamClosed, traceId, "citations", JsonUtils.toJson(payload.getCitations()));
                    safeSend(emitter, streamClosed, traceId, "done", "");
                } catch (Exception ex) {
                    log.warn("streaming cached response failed traceId={}", traceId, ex);
                } finally {
                    safeComplete(emitter, streamClosed);
                }
                return emitter;
            }
        }

        // 未命中缓存：继续走现有流式流程
        RagAnswer ragAnswer = ragAnswerService.retrieveOnly(principal.tenantId(), session.getSessionId(), question);
        // 复用已检索的 chunks 构建 systemPrompt，避免一次请求内重复检索导致重复日志
        String systemPrompt = ragAnswerService.buildSystemPromptForStreaming(question, ragAnswer.getRetrievedChunks());

        // 构建 agent 上下文（短期历史/摘要/长期记忆）
        List<ChatMessage> messages = memoryContextService.buildMessages(
                principal.tenantId(),
                principal.userId(),
                session.getSessionId(),
                userMessage.getMessageId(),
                systemPrompt,
                question
        );

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
                memoryTaskService.enqueuePostAskTasks(
                        principal.tenantId(),
                        principal.userId(),
                        session.getSessionId(),
                        userMessage.getMessageId(),
                        assistantMessage.getMessageId(),
                        question,
                        answerBuf.toString()
                );

                int latency = (int) (System.currentTimeMillis() - start);

                // 写入消息缓存（若缓存已建立）
                chatMessageCacheService.appendIfPresent(principal.tenantId(), principal.userId(), session.getSessionId(), assistantMessage, msgTtl);

                // 摘要任务入队（异步，不阻塞主流程）
                memoryTaskService.enqueueSummaryIfNeeded(principal.tenantId(), principal.userId(), session.getSessionId());

                assistantMessage.setLatencyMs(latency);
                chatMessageMapper.updateById(assistantMessage);

                writeRetrievalLogForRag(principal, traceId, question, ragAnswer, latency);

                session.setLastActiveAt(LocalDateTime.now());
                chatSessionMapper.updateById(session);

                // 仅非高风险才写缓存
                if (!highRiskGuard.isHighRisk(question)) {
                    AnswerCachePayload payload = toCachePayload(question, traceId, ragAnswer);
                    payload.setAnswer(answerBuf.toString());

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
                safeComplete(emitter, streamClosed);
            }

            @Override
            public void onError(Throwable error) {
                if (isClientDisconnect(error)) {
                    log.debug("streaming ask connection closed traceId={}", traceId);
                } else {
                    log.warn("streaming ask failed traceId={}", traceId, error);
                }
                try {
                    send("error", "流式问答连接已中断，请稍后重试");
                } finally {
                    safeComplete(emitter, streamClosed);
                }
            }

            private void send(String type, String data) {
                safeSend(emitter, streamClosed, traceId, type, data);
            }
        }));

        return emitter;
    }

    private void safeSend(SseEmitter emitter, AtomicBoolean streamClosed, String traceId, String type, String data) {
        if (streamClosed.get()) {
            return;
        }
        try {
            emitter.send(SseEmitter.event()
                    .name(type)
                    .data(new AskStreamEvent(type, data)));
        } catch (Exception ex) {
            if (isClientDisconnect(ex)) {
                log.debug("streaming ask client disconnected traceId={} event={}", traceId, type);
            } else {
                log.warn("streaming ask send failed traceId={} event={}", traceId, type, ex);
            }
            safeComplete(emitter, streamClosed);
        }
    }

    private void safeComplete(SseEmitter emitter, AtomicBoolean streamClosed) {
        if (!streamClosed.compareAndSet(false, true)) {
            return;
        }
        try {
            emitter.complete();
        } catch (Exception ignored) {
        }
    }

    private boolean isClientDisconnect(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof SocketException) {
                return true;
            }
            String message = current.getMessage();
            if (message != null) {
                String lowerMessage = message.toLowerCase();
                if (lowerMessage.contains("connection reset")
                        || lowerMessage.contains("broken pipe")
                        || lowerMessage.contains("connection aborted")
                        || lowerMessage.contains("远程主机强迫关闭")
                        || lowerMessage.contains("你的主机中的软件中止了一个已建立的连接")) {
                    return true;
                }
            }
            String className = current.getClass().getName();
            if (className.contains("ClientAbortException")
                    || className.contains("AsyncRequestNotUsableException")) {
                return true;
            }
            current = current instanceof CompletionException && current.getCause() != null
                    ? current.getCause()
                    : current.getCause();
        }
        return false;
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
        java.time.Duration ttl = java.time.Duration.ofDays(Math.max(1, legalMemoryProperties.getCache().getMessagesTtlDays()));
        return chatMessageCacheService.listMessages(principal.tenantId(), principal.userId(), sessionId, ttl);
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
