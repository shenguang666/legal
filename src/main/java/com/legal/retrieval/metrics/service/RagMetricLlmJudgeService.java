package com.legal.retrieval.metrics.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legal.chat.rag.RetrievedChunk;
import com.legal.config.RagRetrievalMetricProperties;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class RagMetricLlmJudgeService {

    private final RagRetrievalMetricProperties properties;
    private final ChatModel chatModel;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RagMetricLlmJudgeService(RagRetrievalMetricProperties properties, @Nullable ChatModel chatModel) {
        this.properties = properties;
        this.chatModel = chatModel;
    }

    public RagMetricJudgeResult judge(String queryText,
                                      List<Long> originalHitChunkIds,
                                      List<RetrievedChunk> candidates) {
        if (chatModel == null) {
            return failed("no-llm");
        }
        String prompt = buildPrompt(queryText, originalHitChunkIds, candidates);
        try {
            String response = chatModel.chat(List.of(
                    SystemMessage.from("你是严谨的RAG检索质量评估器，只返回JSON。"),
                    UserMessage.from(prompt)
            )).aiMessage().text();
            return parse(response, originalHitChunkIds, candidates);
        } catch (Exception ex) {
            return failed("llm-judge-failed:" + ex.getMessage());
        }
    }

    private String buildPrompt(String queryText, List<Long> originalHitChunkIds, List<RetrievedChunk> candidates) {
        StringBuilder builder = new StringBuilder();
        builder.append("请评估检索结果与问题的相关性。只输出严格JSON：")
                .append("{\"relevant_original_chunk_ids\":[1],\"irrelevant_original_chunk_ids\":[2],\"missed_relevant_chunk_ids\":[3],\"explanation\":\"...\"}。")
                .append("\n问题：").append(queryText)
                .append("\n原始命中chunk_id：").append(originalHitChunkIds)
                .append("\n候选切片：\n");
        int maxChars = Math.max(100, properties.getCandidateRecall().getMaxChunkChars());
        for (RetrievedChunk chunk : candidates) {
            String content = chunk.getContent() == null ? "" : chunk.getContent().replaceAll("\\s+", " ").trim();
            if (content.length() > maxChars) {
                content = content.substring(0, maxChars);
            }
            builder.append("chunk_id=").append(chunk.getChunkId())
                    .append(", document_id=").append(chunk.getDocumentId())
                    .append(", content=").append(content)
                    .append("\n");
        }
        log.info("prompt:{}",builder);
        return builder.toString();
    }

    private RagMetricJudgeResult parse(String response,
                                       List<Long> originalHitChunkIds,
                                       List<RetrievedChunk> candidates) throws Exception {
        if (!StringUtils.hasText(response)) {
            throw new IllegalArgumentException("LLM returned empty response");
        }
        JsonNode root = objectMapper.readTree(extractJson(response));
        if (!root.isObject()) {
            throw new IllegalArgumentException("LLM response JSON is not an object");
        }
        if (!root.has("relevant_original_chunk_ids") || !root.has("irrelevant_original_chunk_ids") || !root.has("missed_relevant_chunk_ids")) {
            throw new IllegalArgumentException("LLM response missing required judgment arrays");
        }
        List<Long> relevantOriginal = readIds(root.path("relevant_original_chunk_ids"));
        List<Long> irrelevantOriginal = readIds(root.path("irrelevant_original_chunk_ids"));
        List<Long> missedRelevant = readIds(root.path("missed_relevant_chunk_ids"));
        Set<Long> originalSet = new HashSet<>(originalHitChunkIds);
        Set<Long> candidateSet = new HashSet<>(candidates.stream().map(RetrievedChunk::getChunkId).toList());
        relevantOriginal = relevantOriginal.stream().filter(originalSet::contains).distinct().toList();
        irrelevantOriginal = irrelevantOriginal.stream().filter(originalSet::contains).distinct().toList();
        missedRelevant = missedRelevant.stream()
                .filter(candidateSet::contains)
                .filter(id -> !originalSet.contains(id))
                .distinct()
                .toList();
        return new RagMetricJudgeResult(
                relevantOriginal,
                irrelevantOriginal,
                missedRelevant,
                response,
                root.path("explanation").asText(""),
                true,
                null
        );
    }

    private RagMetricJudgeResult failed(String reason) {
        return new RagMetricJudgeResult(List.of(), List.of(), List.of(), null, "", false, reason);
    }

    private List<Long> readIds(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        for (JsonNode item : node) {
            if (item.canConvertToLong()) {
                ids.add(item.asLong());
            }
        }
        return ids;
    }

    private String extractJson(String text) {
        if (!StringUtils.hasText(text)) {
            throw new IllegalArgumentException("LLM response is blank");
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        throw new IllegalArgumentException("LLM response does not contain JSON object");
    }
}
