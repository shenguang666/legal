package com.legal.memory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legal.config.LegalMemoryProperties;
import com.legal.config.OpenAiChatModelProperties;
import com.legal.enums.UserMemoryStatus;
import com.legal.memory.entity.UserMemoryItemEntity;
import com.legal.memory.mapper.UserMemoryItemMapper;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class LongTermMemoryExtractionService {
    private static final Map<String, String> KEY_ALIAS = Map.of("job", "profession", "职业", "profession", "profession", "profession", "身份", "identity", "identity", "identity", "emotion", "emotion", "情绪", "emotion", "情感", "emotion");
    private final LegalMemoryProperties props; private final UserMemoryItemMapper mapper; private final ChatModel chatModel; private final OpenAiChatModelProperties modelProps; private final ObjectMapper om = new ObjectMapper();
    public LongTermMemoryExtractionService(LegalMemoryProperties props, UserMemoryItemMapper mapper, @Nullable ChatModel chatModel, OpenAiChatModelProperties modelProps) { this.props = props; this.mapper = mapper; this.chatModel = chatModel; this.modelProps = modelProps; }

    public void extractFromSummary(Long tenantId, Long userId, String sessionId, String summaryText) {
        if (!props.getLongTerm().isEnabled() || chatModel == null || !StringUtils.hasText(summaryText)) return;
        String prompt = "请只基于长期对话摘要识别稳定用户信息，不要依据单句主观猜测。只提取 identity、profession、emotion。输出严格 JSON 数组，每项为 {memoryType,memoryKey,memoryValue,sensitivityLevel,confidence}；不确定则不要输出。摘要：" + summaryText;
        try {
            JsonNode arr = om.readTree(chatModel.chat(List.of(SystemMessage.from("你是谨慎的长期记忆抽取器，只返回 JSON"), UserMessage.from(prompt))).aiMessage().text());
            if (!arr.isArray()) return;
            for (JsonNode n : arr) {
                double confidence = n.path("confidence").asDouble(0D);
                if (confidence < props.getLongTerm().getMinConfidence()) continue;
                mergeCandidate(tenantId, userId, sessionId, n.path("memoryType").asText("profile"), canonical(n.path("memoryKey").asText("unknown")), n.path("memoryValue").asText("unknown"), n.path("sensitivityLevel").asText("P1"), confidence);
            }
        } catch (Exception ignored) { }
    }

    private String canonical(String key) { return KEY_ALIAS.getOrDefault(StringUtils.hasText(key) ? key.trim().toLowerCase() : "unknown", "unknown"); }

    private void mergeCandidate(Long tenantId, Long userId, String sessionId, String type, String key, String value, String level, double confidence) {
        if (!StringUtils.hasText(key) || "unknown".equals(key) || !StringUtils.hasText(value) || "unknown".equalsIgnoreCase(value)) return;
        UserMemoryItemEntity e = mapper.selectOne(new LambdaQueryWrapper<UserMemoryItemEntity>().eq(UserMemoryItemEntity::getTenantId, tenantId).eq(UserMemoryItemEntity::getUserId, userId).eq(UserMemoryItemEntity::getMemoryKey, key).last("limit 1"));
        LocalDateTime now = LocalDateTime.now(); int threshold = Math.max(2, props.getLongTerm().getStableThreshold());
        if (e == null) {
            e = new UserMemoryItemEntity(); e.setTenantId(tenantId); e.setUserId(userId); e.setMemoryType(type); e.setMemoryKey(key); e.setVersion(1); e.setCreatedAt(now); e.setConfirmationCount(1); e.setStableThreshold(threshold); e.setStatus(UserMemoryStatus.CANDIDATE);
        } else if (value.equals(e.getMemoryValue())) {
            e.setConfirmationCount((e.getConfirmationCount() == null ? 0 : e.getConfirmationCount()) + 1);
        } else {
            double oldConfidence = e.getConfidence() == null ? 0D : e.getConfidence().doubleValue();
            int oldHits = e.getConfirmationCount() == null ? 0 : e.getConfirmationCount();
            if (oldHits >= threshold || oldConfidence >= confidence - 0.10d) return;
            e.setMemoryValue(value); e.setConfirmationCount(1); e.setStatus(UserMemoryStatus.CANDIDATE); e.setStableSince(null);
        }
        e.setMemoryValue(value); e.setSensitivityLevel(level); e.setConfidence(BigDecimal.valueOf(confidence)); e.setConfirmedByUser(false); e.setSourceSessionId(sessionId); e.setSourceMessageId(null); e.setLastSeenAt(now); e.setModelVersion(modelProps.getModelName()); e.setPromptVersion("memory-extract-from-summary-v3"); e.setUpdatedAt(now);
        if ((e.getConfirmationCount() == null ? 0 : e.getConfirmationCount()) >= threshold) { e.setStatus(UserMemoryStatus.ACTIVE); if (e.getStableSince() == null) e.setStableSince(now); }
        if (e.getId() == null) mapper.insert(e); else mapper.updateById(e);
    }
}
