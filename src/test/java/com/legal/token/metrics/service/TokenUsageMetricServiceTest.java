package com.legal.token.metrics.service;

import com.legal.common.AppException;
import com.legal.config.TokenUsageMetricProperties;
import com.legal.token.metrics.dto.TokenUsageMetricDtos;
import com.legal.token.metrics.entity.TokenUsageMetricDailySummaryEntity;
import com.legal.token.metrics.enums.TokenUsageMetricDailyStatusEnum;
import com.legal.token.metrics.mapper.TokenUsageMetricDailySummaryMapper;
import com.legal.token.metrics.mapper.TokenUsageMetricStatsMapper;
import com.legal.token.metrics.mapper.TokenUsageMetricTopUserMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TokenUsageMetricServiceTest {

    @Test
    void manualEvaluationShouldRejectTodayAndFutureEndDate() {
        TokenUsageMetricService service = serviceWithMocks(mock(TokenUsageMetricDailySummaryMapper.class), mock(TokenUsageMetricStatsMapper.class));

        AppException exception = assertThrows(AppException.class, () ->
                service.evaluateManualRange(1L, LocalDate.now().minusDays(1), LocalDate.now()));

        assertEquals(40001, exception.getCode());
        assertTrue(exception.getMessage().contains("今天之前"));
    }

    @Test
    void manualEvaluationShouldReturnAlreadyProcessedMessage() {
        TokenUsageMetricDailySummaryMapper summaryMapper = mock(TokenUsageMetricDailySummaryMapper.class);
        TokenUsageMetricDailySummaryEntity summary = new TokenUsageMetricDailySummaryEntity();
        summary.setStatus(TokenUsageMetricDailyStatusEnum.COMPLETED.name());
        when(summaryMapper.selectOne(any())).thenReturn(summary);
        TokenUsageMetricService service = serviceWithMocks(summaryMapper, mock(TokenUsageMetricStatsMapper.class));

        TokenUsageMetricDtos.RunResult result = service.evaluateManualRange(1L, LocalDate.now().minusDays(1), null);

        assertTrue(result.isAlreadyProcessed());
        assertEquals("已有记录，无需统计", result.getMessage());
        assertEquals(0, result.getSelected());
        assertEquals(1, result.getAlreadyProcessedDates());
    }

    @Test
    void calculateTenantDateShouldAggregateUsageAndLimitTopUsers() {
        TokenUsageMetricDailySummaryMapper summaryMapper = mock(TokenUsageMetricDailySummaryMapper.class);
        TokenUsageMetricTopUserMapper topUserMapper = mock(TokenUsageMetricTopUserMapper.class);
        TokenUsageMetricStatsMapper statsMapper = mock(TokenUsageMetricStatsMapper.class);
        TokenUsageMetricDtos.AggregatedUsage usage = new TokenUsageMetricDtos.AggregatedUsage();
        usage.setTotalTokenUsage(300L);
        usage.setMessageCount(3L);
        usage.setActiveUserCount(2L);
        TokenUsageMetricDtos.TopUserUsage first = topUser(11L, 200L, 2L);
        TokenUsageMetricDtos.TopUserUsage second = topUser(12L, 100L, 1L);
        when(summaryMapper.selectOne(any())).thenReturn(null);
        when(statsMapper.aggregateDailyUsage(eq(1L), any(), any())).thenReturn(usage);
        when(statsMapper.selectTopUsers(eq(1L), any(), any(), eq(2))).thenReturn(List.of(first, second));
        TokenUsageMetricProperties properties = new TokenUsageMetricProperties();
        properties.getTopUsers().setLimit(2);
        TokenUsageMetricService service = new TokenUsageMetricService(properties, summaryMapper, topUserMapper, statsMapper);

        TokenUsageMetricDailySummaryEntity result = service.calculateTenantDate(1L, LocalDate.now().minusDays(1));

        assertEquals(300L, result.getTotalTokenUsage());
        assertEquals(3L, result.getMessageCount());
        assertEquals(2L, result.getActiveUserCount());
        assertEquals("100.00", result.getAverageTokensPerMessage().toPlainString());
        assertEquals(2, result.getTopUserCount());
        assertEquals(TokenUsageMetricDailyStatusEnum.COMPLETED.name(), result.getStatus());
        verify(statsMapper).selectTopUsers(eq(1L), any(), any(), eq(2));
        verify(topUserMapper).delete(any());
    }

    @Test
    void scheduledRunShouldSkipWhenNoTenantHasUsage() {
        TokenUsageMetricStatsMapper statsMapper = mock(TokenUsageMetricStatsMapper.class);
        when(statsMapper.selectTenantIdsWithUsage(any(), any(), anyInt())).thenReturn(List.of());
        TokenUsageMetricService service = serviceWithMocks(mock(TokenUsageMetricDailySummaryMapper.class), statsMapper);

        TokenUsageMetricDtos.RunResult result = service.evaluateScheduledRun();

        assertEquals(0, result.getSelected());
        assertEquals("no-token-usage", result.getSkippedReason());
    }

    private static TokenUsageMetricService serviceWithMocks(TokenUsageMetricDailySummaryMapper summaryMapper,
                                                            TokenUsageMetricStatsMapper statsMapper) {
        return new TokenUsageMetricService(
                new TokenUsageMetricProperties(),
                summaryMapper,
                mock(TokenUsageMetricTopUserMapper.class),
                statsMapper
        );
    }

    private static TokenUsageMetricDtos.TopUserUsage topUser(Long userId, Long tokenUsage, Long messageCount) {
        TokenUsageMetricDtos.TopUserUsage user = new TokenUsageMetricDtos.TopUserUsage();
        user.setUserId(userId);
        user.setUsername("user" + userId);
        user.setDisplayName("用户" + userId);
        user.setTokenUsage(tokenUsage);
        user.setMessageCount(messageCount);
        return user;
    }
}
