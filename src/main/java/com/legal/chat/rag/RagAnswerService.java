package com.legal.chat.rag;

import com.legal.chat.dto.CitationDto;
import com.legal.chat.dto.CitationImageDto;
import com.legal.chat.memory.ChatMemoryFactory;
import com.legal.common.AppException;
import com.legal.config.OpenAiChatModelProperties;
import com.legal.config.RagProperties;
import com.legal.knowledge.mapper.KbChunkImageRefMapper;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RagAnswerService {

    private static final Logger log = LoggerFactory.getLogger(RagAnswerService.class);

    private final RagProperties ragProperties;
    private final OpenAiChatModelProperties openAiChatModelProperties;
    private final PromptTemplateService promptTemplateService;
    private final ChunkRetriever chunkRetriever;
    private final ChatMemoryFactory chatMemoryFactory;
    private final ChatModel chatModel;
    private final KbChunkImageRefMapper kbChunkImageRefMapper;

    public RagAnswerService(RagProperties ragProperties,
                            OpenAiChatModelProperties openAiChatModelProperties,
                            PromptTemplateService promptTemplateService,
                            ChunkRetriever chunkRetriever,
                            ChatMemoryFactory chatMemoryFactory,
                            KbChunkImageRefMapper kbChunkImageRefMapper,
                            @Nullable ChatModel chatModel) {
        this.ragProperties = ragProperties;
        this.openAiChatModelProperties = openAiChatModelProperties;
        this.promptTemplateService = promptTemplateService;
        this.chunkRetriever = chunkRetriever;
        this.chatMemoryFactory = chatMemoryFactory;
        this.kbChunkImageRefMapper = kbChunkImageRefMapper;
        this.chatModel = chatModel;
    }

    public RagAnswer answer(Long tenantId, String sessionId, String question) {
        if (!ragProperties.isEnabled()) {
            throw AppException.badRequest("当前环境未启用 RAG 功能");
        }
        if (chatModel == null) {
            throw AppException.badRequest("请先配置 LEGAL_LLM_API_KEY 后再使用智能问答");
        }

        List<RetrievedChunk> chunks = chunkRetriever.retrieve(tenantId, question, ragProperties.getTopK());
        String context = buildContext(chunks);
        String knowledgeWarning = chunks.isEmpty() ? ragProperties.getEmptyHitWarning() : "已命中知识库片段，请优先依据知识库内容回答。";
        String systemPrompt = promptTemplateService.renderSystemPrompt(question, context, knowledgeWarning);

        ChatMemory chatMemory = chatMemoryFactory.create(sessionId);
        List<ChatMessage> messages = chatMemory.messages();
        messages.add(SystemMessage.from(systemPrompt));
        messages.add(UserMessage.from(question));

        ChatResponse response = chatModel.chat(messages);
        String answer = response.aiMessage().text();
        int tokenUsage = response.tokenUsage() == null ? 0 : response.tokenUsage().totalTokenCount();
        return new RagAnswer(
                answer,
                openAiChatModelProperties.getModelName(),
                tokenUsage,
                chunks,
                buildCitations(tenantId, chunks),
                !chunks.isEmpty()
        );
    }

    /**
     * 仅执行检索与引用构建，不调用大模型。
     */
    public RagAnswer retrieveOnly(Long tenantId, String sessionId, String question) {
        if (!ragProperties.isEnabled()) {
            throw AppException.badRequest("当前环境未启用 RAG 功能");
        }
        List<RetrievedChunk> chunks = chunkRetriever.retrieve(tenantId, question, ragProperties.getTopK());
        return new RagAnswer(
                "",
                openAiChatModelProperties.getModelName(),
                0,
                chunks,
                buildCitations(tenantId, chunks),
                !chunks.isEmpty()
        );
    }

    /**
     * 供流式接口复用：只构建 systemPrompt（包含召回上下文），不调用模型。
     */
    public String buildSystemPromptForStreaming(Long tenantId, String sessionId, String question) {
        if (!ragProperties.isEnabled()) {
            throw AppException.badRequest("当前环境未启用 RAG 功能");
        }
        List<RetrievedChunk> chunks = chunkRetriever.retrieve(tenantId, question, ragProperties.getTopK());
        return buildSystemPromptForStreaming(question, chunks);
    }

    /**
     * 流式接口复用：当上层已完成检索时（例如 askStream 先 retrieveOnly），
     * 可直接复用 chunks 构建 systemPrompt，避免同一请求内重复检索导致重复日志与额外开销。
     */
    public String buildSystemPromptForStreaming(String question, List<RetrievedChunk> chunks) {
        List<RetrievedChunk> safeChunks = chunks == null ? List.of() : chunks;
        String context = buildContext(safeChunks);
        String knowledgeWarning = safeChunks.isEmpty() ? ragProperties.getEmptyHitWarning() : "已命中知识库片段，请优先依据知识库内容回答。";
        return promptTemplateService.renderSystemPrompt(question, context, knowledgeWarning);
    }

    /**
     * 供流式接口复用：创建会话记忆。
     */
    public ChatMemory createMemoryForStreaming(String sessionId) {
        return chatMemoryFactory.create(sessionId);
    }

    private String buildContext(List<RetrievedChunk> chunks) {
        if (chunks.isEmpty()) {
            return "当前没有命中的知识库片段。";
        }
        return chunks.stream()
                .map(chunk -> "[documentId=" + chunk.getDocumentId()
                        + ", chunkId=" + chunk.getChunkId()
                        + ", order=" + chunk.getChunkOrder()
                        + ", source=" + chunk.getSource() + "]\n"
                        + chunk.getContent())
                .collect(Collectors.joining("\n\n"));
    }

    private List<CitationDto> buildCitations(Long tenantId, List<RetrievedChunk> chunks) {
        Map<Long, List<CitationImageDto>> imagesByChunkId = loadImagesByChunkId(tenantId, chunks);
        return chunks.stream()
                .map(chunk -> new CitationDto(
                        chunk.getDocumentId(),
                        chunk.getChunkId(),
                        chunk.getSource(),
                        shorten(chunk.getContent()),
                        imagesByChunkId.getOrDefault(chunk.getChunkId(), List.of())
                ))
                .collect(Collectors.toList());
    }

    private Map<Long, List<CitationImageDto>> loadImagesByChunkId(Long tenantId, List<RetrievedChunk> chunks) {
        List<Long> chunkIds = chunks.stream()
                .map(RetrievedChunk::getChunkId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (chunkIds.isEmpty()) {
            return Map.of();
        }
        List<KbChunkImageRefMapper.ChunkImageAssetRow> rows;
        try {
            rows = kbChunkImageRefMapper.selectImageRowsByChunkIds(tenantId, chunkIds);
        } catch (RuntimeException ex) {
            if (!isMissingImageRefTable(ex)) {
                throw ex;
            }
            log.warn("kb_chunk_image_ref 表不存在，RAG citation 暂不返回图片证据");
            return Map.of();
        }
        Map<Long, List<CitationImageDto>> result = new LinkedHashMap<>();
        for (KbChunkImageRefMapper.ChunkImageAssetRow row : rows) {
            result.computeIfAbsent(row.getChunkId(), ignored -> new ArrayList<>())
                    .add(new CitationImageDto(
                            row.getImageAssetId(),
                            row.getPublicUrl(),
                            row.getDescription(),
                            row.getOriginalPath(),
                            row.getImageOrder()
                    ));
        }
        return result;
    }

    private boolean isMissingImageRefTable(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.contains("kb_chunk_image_ref") && message.toLowerCase().contains("doesn't exist")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String shorten(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 120 ? normalized : normalized.substring(0, 120) + "...";
    }
}
