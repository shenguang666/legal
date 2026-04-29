package com.legal.chat.rag;

import com.legal.chat.dto.CitationDto;
import com.legal.chat.memory.ChatMemoryFactory;
import com.legal.common.AppException;
import com.legal.config.OpenAiChatModelProperties;
import com.legal.config.RagProperties;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagAnswerService {

    private final RagProperties ragProperties;
    private final OpenAiChatModelProperties openAiChatModelProperties;
    private final PromptTemplateService promptTemplateService;
    private final ChunkRetriever chunkRetriever;
    private final ChatMemoryFactory chatMemoryFactory;
    private final ChatModel chatModel;

    public RagAnswerService(RagProperties ragProperties,
                            OpenAiChatModelProperties openAiChatModelProperties,
                            PromptTemplateService promptTemplateService,
                            ChunkRetriever chunkRetriever,
                            ChatMemoryFactory chatMemoryFactory,
                            @Nullable ChatModel chatModel) {
        this.ragProperties = ragProperties;
        this.openAiChatModelProperties = openAiChatModelProperties;
        this.promptTemplateService = promptTemplateService;
        this.chunkRetriever = chunkRetriever;
        this.chatMemoryFactory = chatMemoryFactory;
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
                chunks.stream()
                        .map(chunk -> new CitationDto(chunk.getDocumentId(), chunk.getSource(), shorten(chunk.getContent())))
                        .collect(Collectors.toList()),
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
                chunks.stream()
                        .map(chunk -> new CitationDto(chunk.getDocumentId(), chunk.getSource(), shorten(chunk.getContent())))
                        .collect(Collectors.toList()),
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
        String context = buildContext(chunks);
        String knowledgeWarning = chunks.isEmpty() ? ragProperties.getEmptyHitWarning() : "已命中知识库片段，请优先依据知识库内容回答。";
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

    private String shorten(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 120 ? normalized : normalized.substring(0, 120) + "...";
    }
}
