package com.legal.retrieval.metrics.service;

import com.legal.chat.rag.ChunkRetriever;
import com.legal.chat.rag.RetrievedChunk;
import com.legal.config.RagRetrievalMetricProperties;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagRetrievalMetricServicesTest {

    @Test
    void calculatorShouldUseStandardRecallAndPrecisionFormulas() {
        assertEquals(2D / 3D, RagRetrievalMetricCalculator.recall(2, 1), 0.0001D);
        assertEquals(2D / 3D, RagRetrievalMetricCalculator.precision(2, 1), 0.0001D);
        assertEquals(0D, RagRetrievalMetricCalculator.recall(0, 0), 0.0001D);
        assertEquals(0D, RagRetrievalMetricCalculator.precision(0, 0), 0.0001D);
    }

    @Test
    void judgeShouldParseRelevantIrrelevantAndMissedChunkIds() {
        ChatModel chatModel = new FakeChatModel("{\"relevant_original_chunk_ids\":[1],\"irrelevant_original_chunk_ids\":[2],\"missed_relevant_chunk_ids\":[3],\"explanation\":\"ok\"}");
        RagMetricLlmJudgeService service = new RagMetricLlmJudgeService(new RagRetrievalMetricProperties(), chatModel);

        RagMetricJudgeResult result = service.judge(
                "合同金额是多少",
                List.of(1L, 2L),
                List.of(chunk(1L), chunk(2L), chunk(3L))
        );

        assertEquals(List.of(1L), result.relevantOriginalChunkIds());
        assertEquals(List.of(2L), result.irrelevantOriginalChunkIds());
        assertEquals(List.of(3L), result.missedRelevantChunkIds());
        assertEquals("ok", result.explanation());
        assertTrue(result.success());
    }

    @Test
    void judgeShouldFailWhenLlmIsUnavailable() {
        RagMetricLlmJudgeService service = new RagMetricLlmJudgeService(new RagRetrievalMetricProperties(), null);

        RagMetricJudgeResult result = service.judge(
                "付款日期是什么",
                List.of(10L, 11L),
                List.of(chunk(10L))
        );

        assertFalse(result.success());
        assertEquals("no-llm", result.failureReason());
        assertEquals(List.of(), result.relevantOriginalChunkIds());
        assertEquals(List.of(), result.irrelevantOriginalChunkIds());
        assertEquals(List.of(), result.missedRelevantChunkIds());
    }

    @Test
    void judgeShouldFailWhenLlmResponseIsInvalid() {
        RagMetricLlmJudgeService service = new RagMetricLlmJudgeService(new RagRetrievalMetricProperties(), new FakeChatModel(""));

        RagMetricJudgeResult result = service.judge(
                "付款日期是什么",
                List.of(10L, 11L),
                List.of(chunk(10L), chunk(11L))
        );

        assertFalse(result.success());
        assertTrue(result.failureReason().contains("LLM returned empty response"));
    }

    @Test
    void candidateRecallShouldUseRequestedTopNAndKeepChunkIdentity() {
        RagMetricCandidateRecallService service = new RagMetricCandidateRecallService(new StubChunkRetriever());

        List<RetrievedChunk> chunks = service.recall(1L, "合同金额是多少", 2);

        assertEquals(2, chunks.size());
        assertEquals(1L, chunks.get(0).getChunkId());
        assertEquals(2L, chunks.get(1).getChunkId());
    }

    private static RetrievedChunk chunk(Long chunkId) {
        return new RetrievedChunk(chunkId, 100L + chunkId, 1, "test", "chunk-" + chunkId);
    }

    static class FakeChatModel implements ChatModel {
        private final String response;

        FakeChatModel(String response) {
            this.response = response;
        }

        @Override
        public ChatResponse chat(List<ChatMessage> messages) {
            return ChatResponse.builder().aiMessage(AiMessage.from(response)).build();
        }
    }

    static class StubChunkRetriever implements ChunkRetriever {
        @Override
        public List<RetrievedChunk> retrieve(Long tenantId, String question, int topK) {
            return List.of(chunk(1L), chunk(2L), chunk(3L)).stream()
                    .limit(topK)
                    .toList();
        }
    }
}
