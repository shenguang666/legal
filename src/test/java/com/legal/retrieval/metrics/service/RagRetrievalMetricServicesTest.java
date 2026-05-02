package com.legal.retrieval.metrics.service;

import com.legal.chat.rag.ChunkRetriever;
import com.legal.chat.rag.RetrievedChunk;
import com.legal.common.AppException;
import com.legal.config.OpenAiChatModelProperties;
import com.legal.config.RagRetrievalMetricProperties;
import com.legal.retrieval.entity.RetrievalLogEntity;
import com.legal.retrieval.mapper.RetrievalLogMapper;
import com.legal.retrieval.metrics.dto.RagRetrievalMetricDtos;
import com.legal.retrieval.metrics.entity.RagRetrievalMetricDailySummaryEntity;
import com.legal.retrieval.metrics.entity.RagRetrievalMetricEvaluationEntity;
import com.legal.retrieval.metrics.enums.RagMetricDailyStatusEnum;
import com.legal.retrieval.metrics.enums.RagMetricScanStatusEnum;
import com.legal.retrieval.metrics.mapper.RagRetrievalMetricDailySummaryMapper;
import com.legal.retrieval.metrics.mapper.RagRetrievalMetricEvaluationMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test
    void manualEvaluationShouldRejectTodayAndFutureEndDate() {
        RagRetrievalMetricService service = serviceWithMocks(mock(RetrievalLogMapper.class), mock(RagRetrievalMetricDailySummaryMapper.class));

        AppException exception = assertThrows(AppException.class, () ->
                service.evaluateManualRange(1L, LocalDate.now().minusDays(1), LocalDate.now()));

        assertEquals(40001, exception.getCode());
        assertTrue(exception.getMessage().contains("今天之前"));
    }

    @Test
    void manualEvaluationShouldReturnAlreadyProcessedMessage() {
        RetrievalLogMapper retrievalLogMapper = mock(RetrievalLogMapper.class);
        RagRetrievalMetricDailySummaryMapper dailySummaryMapper = mock(RagRetrievalMetricDailySummaryMapper.class);
        RagRetrievalMetricDailySummaryEntity summary = new RagRetrievalMetricDailySummaryEntity();
        summary.setStatus(RagMetricDailyStatusEnum.COMPLETED.name());
        when(retrievalLogMapper.selectCount(any())).thenReturn(0L);
        when(dailySummaryMapper.selectOne(any())).thenReturn(summary);
        RagRetrievalMetricService service = serviceWithMocks(retrievalLogMapper, dailySummaryMapper);

        RagRetrievalMetricDtos.RunResult result = service.evaluateManualRange(1L, LocalDate.now().minusDays(1), null);

        assertTrue(result.isAlreadyProcessed());
        assertEquals("已有记录，无需评估", result.getMessage());
        assertEquals(0, result.getSelected());
        assertEquals(1, result.getAlreadyProcessedDates());
    }

    @Test
    void manualEvaluationShouldSkipWhenTerminalLogsExistWithoutDailySummary() {
        RetrievalLogMapper retrievalLogMapper = mock(RetrievalLogMapper.class);
        RagRetrievalMetricDailySummaryMapper dailySummaryMapper = mock(RagRetrievalMetricDailySummaryMapper.class);
        when(retrievalLogMapper.selectCount(any())).thenReturn(0L, 1L);
        when(dailySummaryMapper.selectOne(any())).thenReturn(null);
        RagRetrievalMetricService service = serviceWithMocks(retrievalLogMapper, dailySummaryMapper);

        RagRetrievalMetricDtos.RunResult result = service.evaluateManualRange(1L, LocalDate.now().minusDays(1), null);

        assertTrue(result.isAlreadyProcessed());
        assertEquals("已有记录，无需评估", result.getMessage());
        assertEquals(0, result.getSelected());
        assertEquals(1, result.getAlreadyProcessedDates());
    }
    @Test
    void scheduledRunShouldBackfillOlderProcessableDate() {
        LocalDate oldDate = LocalDate.now().minusDays(3);
        RetrievalLogEntity log = new RetrievalLogEntity();
        log.setId(10L);
        log.setTenantId(1L);
        log.setQueryText("合同金额是多少");
        log.setHitChunkIds("1");
        log.setCreatedAt(oldDate.atTime(9, 0));
        log.setRagMetricScanStatus(RagMetricScanStatusEnum.PENDING.name());
        RagRetrievalMetricDailySummaryEntity summary = new RagRetrievalMetricDailySummaryEntity();
        summary.setId(99L);
        summary.setTenantId(1L);
        summary.setMetricDate(oldDate);
        summary.setFailedCount(0L);
        RetrievalLogMapper retrievalLogMapper = mock(RetrievalLogMapper.class);
        RagRetrievalMetricDailySummaryMapper dailySummaryMapper = mock(RagRetrievalMetricDailySummaryMapper.class);
        RagRetrievalMetricEvaluationMapper evaluationMapper = mock(RagRetrievalMetricEvaluationMapper.class);
        RagMetricCandidateRecallService recallService = mock(RagMetricCandidateRecallService.class);
        RagMetricLlmJudgeService judgeService = mock(RagMetricLlmJudgeService.class);
        when(retrievalLogMapper.selectCount(any())).thenReturn(0L, 0L, 1L, 0L);
        when(retrievalLogMapper.selectList(any())).thenReturn(List.of(log), List.of(log));
        when(dailySummaryMapper.selectOne(any())).thenReturn(summary);
        when(dailySummaryMapper.selectById(any())).thenReturn(summary);
        when(dailySummaryMapper.selectList(any())).thenReturn(List.of(summary));
        when(evaluationMapper.selectCount(any())).thenReturn(1L, 0L, 0L);
        RagRetrievalMetricEvaluationEntity success = new RagRetrievalMetricEvaluationEntity();
        success.setRecallScore(BigDecimal.ONE);
        success.setPrecisionScore(BigDecimal.ONE);
        when(evaluationMapper.selectList(any())).thenReturn(List.of(success));
        when(recallService.recall(any(), any(), anyInt())).thenReturn(List.of(chunk(1L)));
        when(judgeService.judge(any(), any(), any())).thenReturn(new RagMetricJudgeResult(
                List.of(1L), List.of(), List.of(), "{}", "ok", true, null));
        RagRetrievalMetricService service = serviceWithMocks(retrievalLogMapper, dailySummaryMapper, evaluationMapper, recallService, judgeService);

        RagRetrievalMetricDtos.RunResult result = service.evaluateScheduledRun();

        assertTrue(result.isBackfill());
        assertEquals(oldDate, result.getMetricDate());
        assertEquals(1, result.getSelected());
        assertEquals(1, result.getSuccess());
    }

    private static RetrievedChunk chunk(Long chunkId) {
        return new RetrievedChunk(chunkId, 100L + chunkId, 1, "test", "chunk-" + chunkId);
    }

    private static RagRetrievalMetricService serviceWithMocks(RetrievalLogMapper retrievalLogMapper,
                                                              RagRetrievalMetricDailySummaryMapper dailySummaryMapper) {
        return serviceWithMocks(
                retrievalLogMapper,
                dailySummaryMapper,
                mock(RagRetrievalMetricEvaluationMapper.class),
                mock(RagMetricCandidateRecallService.class),
                mock(RagMetricLlmJudgeService.class)
        );
    }

    private static RagRetrievalMetricService serviceWithMocks(RetrievalLogMapper retrievalLogMapper,
                                                              RagRetrievalMetricDailySummaryMapper dailySummaryMapper,
                                                              RagRetrievalMetricEvaluationMapper evaluationMapper,
                                                              RagMetricCandidateRecallService recallService,
                                                              RagMetricLlmJudgeService judgeService) {
        OpenAiChatModelProperties chatProperties = new OpenAiChatModelProperties();
        chatProperties.setModelName("test-model");
        return new RagRetrievalMetricService(
                new RagRetrievalMetricProperties(),
                chatProperties,
                retrievalLogMapper,
                dailySummaryMapper,
                evaluationMapper,
                recallService,
                judgeService
        );
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
