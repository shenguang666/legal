package com.legal.memory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legal.config.LegalMemoryProperties;
import com.legal.memory.entity.UserKnowledgeEntity;
import com.legal.memory.mapper.UserKnowledgeMapper;
import com.legal.retrieval.service.OpenAiEmbeddingClient;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserKnowledgeService {

    private final LegalMemoryProperties props;
    private final UserKnowledgeMapper userKnowledgeMapper;
    private final OpenAiEmbeddingClient embeddingClient;
    private final ElasticsearchUserKnowledgeStore userKnowledgeStore;
    private final ChatModel chatModel;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public UserKnowledgeService(LegalMemoryProperties props, UserKnowledgeMapper userKnowledgeMapper,
                                OpenAiEmbeddingClient embeddingClient, ElasticsearchUserKnowledgeStore userKnowledgeStore,
                                @Nullable ChatModel chatModel) {
        this.props = props; this.userKnowledgeMapper = userKnowledgeMapper; this.embeddingClient = embeddingClient;
        this.userKnowledgeStore = userKnowledgeStore; this.chatModel = chatModel;
    }

    public void classifyAndStore(Long tenantId, Long userId, String sessionId, MemoryTaskPayloads.QaPayload payload) {
        if (!props.getUserKnowledge().isEnabled() || payload == null || !allowByRule(payload.getQuestion(), payload.getAnswer())) return;
        Decision d = classify(payload.getQuestion(), payload.getAnswer());
        if (d == null || "FORBIDDEN".equals(d.level) || !StringUtils.hasText(d.coreContent)) return;
        if (existsSameCore(tenantId, userId, d.coreContent)) return;
        UserKnowledgeEntity entity = buildEntity(tenantId, userId, sessionId, payload, d);
        userKnowledgeMapper.insert(entity);
        if ("MUST".equals(d.level)) activateAndIndex(entity);
    }

    public void reviewPendingBySummary(Long tenantId, Long userId, String sessionId, String summaryText) {
        if (!props.getUserKnowledge().isEnabled() || !StringUtils.hasText(summaryText) || chatModel == null) return;
        List<UserKnowledgeEntity> pending = userKnowledgeMapper.selectList(new LambdaQueryWrapper<UserKnowledgeEntity>()
                .eq(UserKnowledgeEntity::getTenantId, tenantId).eq(UserKnowledgeEntity::getUserId, userId)
                .eq(UserKnowledgeEntity::getSessionId, sessionId).eq(UserKnowledgeEntity::getStatus, "PENDING"));
        for (UserKnowledgeEntity item : pending) {
            String judge = chatModel.chat(List.of(SystemMessage.from("你是知识复核器，只回答 MUST 或 FORBIDDEN"),
                    UserMessage.from("基于会话摘要判断下面候选知识是否应入用户外挂知识库。若是长期稳定/可复用增量知识返回 MUST，否则返回 FORBIDDEN。\n摘要："
                            + summaryText + "\n候选：" + item.getCoreContent()))).aiMessage().text();
            if (judge != null && judge.toUpperCase().contains("MUST")) activateAndIndex(item);
            else { item.setStatus("REJECTED"); item.setReviewedAt(LocalDateTime.now()); userKnowledgeMapper.updateById(item); }
        }
    }

    public List<String> searchRelevant(Long tenantId, Long userId, String question) {
        if (!props.getUserKnowledge().isEnabled() || !StringUtils.hasText(question)) return List.of();
        try { return userKnowledgeStore.search(tenantId, userId, embeddingClient.embed(question), props.getUserKnowledge().getTopK()); }
        catch (Exception ex) { return userKnowledgeMapper.selectList(new LambdaQueryWrapper<UserKnowledgeEntity>()
                .eq(UserKnowledgeEntity::getTenantId, tenantId).eq(UserKnowledgeEntity::getUserId, userId)
                .eq(UserKnowledgeEntity::getStatus, "ACTIVE").orderByDesc(UserKnowledgeEntity::getCreatedAt)
                .last("limit " + Math.max(1, props.getUserKnowledge().getTopK()))).stream().map(UserKnowledgeEntity::getCoreContent).filter(StringUtils::hasText).toList(); }
    }

    private boolean allowByRule(String q, String a) { return StringUtils.hasText(q) && q.length() >= props.getUserKnowledge().getMinQuestionLength() && StringUtils.hasText(a) && a.length() >= 20; }
    private boolean existsSameCore(Long tenantId, Long userId, String core) { return userKnowledgeMapper.selectCount(new LambdaQueryWrapper<UserKnowledgeEntity>().eq(UserKnowledgeEntity::getTenantId, tenantId).eq(UserKnowledgeEntity::getUserId, userId).eq(UserKnowledgeEntity::getCoreContent, core).last("limit 1")) > 0; }
    private UserKnowledgeEntity buildEntity(Long tenantId, Long userId, String sessionId, MemoryTaskPayloads.QaPayload payload, Decision d) {
        UserKnowledgeEntity e = new UserKnowledgeEntity(); e.setTenantId(tenantId); e.setUserId(userId); e.setSessionId(sessionId); e.setQuestion(payload.getQuestion()); e.setAnswer(payload.getAnswer()); e.setContent("问题：" + payload.getQuestion() + "\n回答：" + payload.getAnswer()); e.setSource("用户会话外挂知识"); e.setKnowledgeLevel(d.level); e.setReason(d.reason); e.setCoreContent(d.coreContent); e.setStatus("MUST".equals(d.level) ? "ACTIVE" : "PENDING"); e.setIndexStatus("PENDING"); e.setSourceUserMessageId(payload.getUserMessageId()); e.setSourceAssistantMessageId(payload.getAssistantMessageId()); e.setCreatedAt(LocalDateTime.now()); e.setUpdatedAt(LocalDateTime.now()); return e; }
    private void activateAndIndex(UserKnowledgeEntity e) { String content = StringUtils.hasText(e.getCoreContent()) ? e.getCoreContent() : e.getContent(); userKnowledgeStore.upsert(e.getKnowledgeId(), e.getTenantId(), e.getUserId(), e.getSource(), content, embeddingClient.embed(content)); e.setStatus("ACTIVE"); e.setIndexStatus("COMPLETED"); e.setReviewedAt(LocalDateTime.now()); e.setUpdatedAt(LocalDateTime.now()); userKnowledgeMapper.updateById(e); }

    private Decision classify(String question, String answer) {
        if (chatModel == null) return new Decision("OPTIONAL", "no-llm", question + "\n" + answer);
        String prompt = "你是对话信息价值判断专家。只输出 JSON：{\"level\":\"must/optional/forbidden\",\"reason\":\"...\",\"core_content\":\"...\"}。入库标准参考：用户明确长期记住的信息、未来可复用增量知识、跨轮稳定需求；禁止寒暄、临时指令、错误/重复/敏感内容。\n用户问题：" + question + "\n助手回答：" + answer;
        try { JsonNode n = objectMapper.readTree(chatModel.chat(List.of(SystemMessage.from("你是知识价值分类器，只返回 JSON"), UserMessage.from(prompt))).aiMessage().text()); return new Decision(n.path("level").asText("forbidden").toUpperCase(), n.path("reason").asText(""), n.path("core_content").asText("")); } catch (Exception ex) { return null; }
    }

    private record Decision(String level, String reason, String coreContent) { }
}
