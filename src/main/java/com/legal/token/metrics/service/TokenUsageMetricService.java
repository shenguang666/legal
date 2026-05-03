package com.legal.token.metrics.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.legal.common.AppException;
import com.legal.config.TokenUsageMetricProperties;
import com.legal.token.metrics.dto.TokenUsageMetricDtos;
import com.legal.token.metrics.entity.TokenUsageMetricDailySummaryEntity;
import com.legal.token.metrics.entity.TokenUsageMetricTopUserEntity;
import com.legal.token.metrics.enums.TokenUsageMetricDailyStatusEnum;
import com.legal.token.metrics.mapper.TokenUsageMetricDailySummaryMapper;
import com.legal.token.metrics.mapper.TokenUsageMetricStatsMapper;
import com.legal.token.metrics.mapper.TokenUsageMetricTopUserMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

@Service
public class TokenUsageMetricService {

    private final TokenUsageMetricProperties properties;
    private final TokenUsageMetricDailySummaryMapper dailySummaryMapper;
    private final TokenUsageMetricTopUserMapper topUserMapper;
    private final TokenUsageMetricStatsMapper statsMapper;

    public TokenUsageMetricService(TokenUsageMetricProperties properties,
                                   TokenUsageMetricDailySummaryMapper dailySummaryMapper,
                                   TokenUsageMetricTopUserMapper topUserMapper,
                                   TokenUsageMetricStatsMapper statsMapper) {
        this.properties = properties;
        this.dailySummaryMapper = dailySummaryMapper;
        this.topUserMapper = topUserMapper;
        this.statsMapper = statsMapper;
    }

    public TokenUsageMetricDtos.RunResult evaluateScheduledRun() {
        LocalDate metricDate = scheduledMetricDate();
        LocalDateTime startAt = metricDate.atStartOfDay();
        LocalDateTime endAt = metricDate.atTime(LocalTime.MAX);
        List<Long> tenantIds = statsMapper.selectTenantIdsWithUsage(startAt, endAt, Math.max(1, properties.getWorker().getBatchSize()));
        TokenUsageMetricDtos.RunResult result = new TokenUsageMetricDtos.RunResult();
        result.setMetricDate(metricDate);
        if (tenantIds.isEmpty()) {
            result.setMessage("当前归档日期暂无 Token 消耗消息");
            result.setSkippedReason("no-token-usage");
            return result;
        }
        for (Long tenantId : tenantIds) {
            if (isDateAlreadyProcessed(tenantId, metricDate)) {
                result.setSkipped(result.getSkipped() + 1);
                result.setAlreadyProcessedDates(result.getAlreadyProcessedDates() + 1);
                continue;
            }
            result.setSelected(result.getSelected() + 1);
            try {
                calculateTenantDate(tenantId, metricDate);
                result.setSuccess(result.getSuccess() + 1);
            } catch (Exception ex) {
                result.setFailed(result.getFailed() + 1);
                result.setMessage(truncate(ex.getMessage(), 500));
            }
        }
        result.setMessage("Token 消耗定时统计完成");
        return result;
    }

    public TokenUsageMetricDtos.RunResult evaluateManualRange(Long tenantId, LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();
        LocalDate safeEnd = endDate == null ? startDate : endDate;
        LocalDate safeStart = startDate == null ? safeEnd : startDate;
        if (safeStart == null || safeEnd == null) {
            throw AppException.badRequest("请选择需要手动统计的历史日期");
        }
        if (safeEnd.isBefore(safeStart)) {
            throw AppException.badRequest("手动统计结束日期不能早于开始日期");
        }
        if (!safeEnd.isBefore(today)) {
            throw AppException.badRequest("手动统计结束日期只能选择今天之前的历史日期");
        }
        TokenUsageMetricDtos.RunResult merged = new TokenUsageMetricDtos.RunResult();
        merged.setStartDate(safeStart);
        merged.setEndDate(safeEnd);
        LocalDate cursor = safeStart;
        while (!cursor.isAfter(safeEnd)) {
            if (isDateAlreadyProcessed(tenantId, cursor)) {
                merged.setSkipped(merged.getSkipped() + 1);
                merged.setAlreadyProcessedDates(merged.getAlreadyProcessedDates() + 1);
                cursor = cursor.plusDays(1);
                continue;
            }
            merged.setSelected(merged.getSelected() + 1);
            try {
                calculateTenantDate(tenantId, cursor);
                merged.setSuccess(merged.getSuccess() + 1);
            } catch (Exception ex) {
                merged.setFailed(merged.getFailed() + 1);
                merged.setMessage(truncate(ex.getMessage(), 500));
            }
            cursor = cursor.plusDays(1);
        }
        merged.setAlreadyProcessed(merged.getAlreadyProcessedDates() > 0 && merged.getSelected() == 0);
        if (merged.isAlreadyProcessed()) {
            merged.setMessage("已有记录，无需统计");
            merged.setSkippedReason("already-processed");
        } else if (merged.getSelected() == 0) {
            merged.setMessage("暂无需要统计的 Token 消耗日期");
            merged.setSkippedReason("no-pending-dates");
        } else if (merged.getAlreadyProcessedDates() > 0) {
            merged.setMessage("部分日期已有记录，已跳过；其余日期统计完成");
        } else {
            merged.setMessage("手动统计完成");
        }
        return merged;
    }

    public TokenUsageMetricDtos.Summary summary(Long tenantId, LocalDate startDate, LocalDate endDate) {
        List<TokenUsageMetricDailySummaryEntity> summaries = selectSummaries(tenantId, startDate, endDate);
        long totalTokens = sumLong(summaries, TokenUsageMetricDailySummaryEntity::getTotalTokenUsage);
        long messageCount = sumLong(summaries, TokenUsageMetricDailySummaryEntity::getMessageCount);
        TokenUsageMetricDtos.Summary summary = new TokenUsageMetricDtos.Summary();
        summary.setTotalTokenUsage(totalTokens);
        summary.setMessageCount(messageCount);
        summary.setActiveUserCount(sumLong(summaries, TokenUsageMetricDailySummaryEntity::getActiveUserCount));
        summary.setAverageTokensPerMessage(averageTokens(totalTokens, messageCount));
        summary.setMetricVersion(metricVersion());
        summary.setTrends(summaries.stream().map(this::toTrendPoint).toList());
        summary.setDailySummaries(summaries.stream().map(this::toDailySummary).toList());
        return summary;
    }

    public TokenUsageMetricDtos.DailySummaryPage dailySummaries(Long tenantId, LocalDate startDate, LocalDate endDate, int pageNo, int pageSize) {
        int safePageNo = Math.max(1, pageNo);
        int safePageSize = Math.max(1, Math.min(pageSize, 100));
        LambdaQueryWrapper<TokenUsageMetricDailySummaryEntity> wrapper = summaryWrapper(tenantId, startDate, endDate)
                .orderByDesc(TokenUsageMetricDailySummaryEntity::getMetricDate);
        Page<TokenUsageMetricDailySummaryEntity> page = dailySummaryMapper.selectPage(Page.of(safePageNo, safePageSize), wrapper);
        TokenUsageMetricDtos.DailySummaryPage result = new TokenUsageMetricDtos.DailySummaryPage();
        result.setTotal(page.getTotal());
        result.setPageNo(safePageNo);
        result.setPageSize(safePageSize);
        result.setItems(page.getRecords().stream().map(this::toDailySummary).toList());
        return result;
    }

    public List<TokenUsageMetricDtos.TopUser> topUsers(Long tenantId, Long dailySummaryId) {
        return topUserMapper.selectList(new LambdaQueryWrapper<TokenUsageMetricTopUserEntity>()
                        .eq(TokenUsageMetricTopUserEntity::getTenantId, tenantId)
                        .eq(TokenUsageMetricTopUserEntity::getDailySummaryId, dailySummaryId)
                        .orderByAsc(TokenUsageMetricTopUserEntity::getRankNo))
                .stream()
                .map(this::toTopUser)
                .toList();
    }

    @Transactional
    public TokenUsageMetricDailySummaryEntity calculateTenantDate(Long tenantId, LocalDate metricDate) {
        TokenUsageMetricDailySummaryEntity summary = getOrCreateDailySummary(tenantId, metricDate);
        markProcessing(summary);
        try {
            LocalDateTime startAt = metricDate.atStartOfDay();
            LocalDateTime endAt = metricDate.atTime(LocalTime.MAX);
            TokenUsageMetricDtos.AggregatedUsage usage = statsMapper.aggregateDailyUsage(tenantId, startAt, endAt);
            long totalTokens = nullToZero(usage == null ? null : usage.getTotalTokenUsage());
            long messageCount = nullToZero(usage == null ? null : usage.getMessageCount());
            long activeUserCount = nullToZero(usage == null ? null : usage.getActiveUserCount());
            List<TokenUsageMetricDtos.TopUserUsage> topUsers = statsMapper.selectTopUsers(
                    tenantId, startAt, endAt, Math.max(1, properties.getTopUsers().getLimit()));
            rebuildTopUsers(summary, topUsers, totalTokens);
            summary.setTotalTokenUsage(totalTokens);
            summary.setMessageCount(messageCount);
            summary.setActiveUserCount(activeUserCount);
            summary.setAverageTokensPerMessage(averageTokens(totalTokens, messageCount));
            summary.setTopUserLimit(Math.max(1, properties.getTopUsers().getLimit()));
            summary.setTopUserCount(topUsers.size());
            summary.setStatus(TokenUsageMetricDailyStatusEnum.COMPLETED.name());
            summary.setErrorMessage(null);
            summary.setCompletedAt(LocalDateTime.now());
            summary.setUpdatedAt(LocalDateTime.now());
            dailySummaryMapper.updateById(summary);
            return summary;
        } catch (Exception ex) {
            summary.setStatus(TokenUsageMetricDailyStatusEnum.FAILED.name());
            summary.setErrorMessage(truncate(ex.getMessage(), 1000));
            summary.setRetryCount((summary.getRetryCount() == null ? 0 : summary.getRetryCount()) + 1);
            summary.setCompletedAt(LocalDateTime.now());
            summary.setUpdatedAt(LocalDateTime.now());
            dailySummaryMapper.updateById(summary);
            if (ex instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException(ex);
        }
    }

    private TokenUsageMetricDailySummaryEntity getOrCreateDailySummary(Long tenantId, LocalDate metricDate) {
        TokenUsageMetricDailySummaryEntity existing = selectDailySummary(tenantId, metricDate);
        if (existing != null) {
            return existing;
        }
        TokenUsageMetricDailySummaryEntity entity = new TokenUsageMetricDailySummaryEntity();
        LocalDateTime now = LocalDateTime.now();
        entity.setTenantId(tenantId);
        entity.setMetricDate(metricDate);
        entity.setMetricVersion(metricVersion());
        entity.setTotalTokenUsage(0L);
        entity.setMessageCount(0L);
        entity.setActiveUserCount(0L);
        entity.setAverageTokensPerMessage(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        entity.setTopUserLimit(Math.max(1, properties.getTopUsers().getLimit()));
        entity.setTopUserCount(0);
        entity.setStatus(TokenUsageMetricDailyStatusEnum.PROCESSING.name());
        entity.setRetryCount(0);
        entity.setStartedAt(now);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        try {
            dailySummaryMapper.insert(entity);
            return entity;
        } catch (DuplicateKeyException ex) {
            return selectDailySummary(tenantId, metricDate);
        }
    }

    private void markProcessing(TokenUsageMetricDailySummaryEntity summary) {
        summary.setStatus(TokenUsageMetricDailyStatusEnum.PROCESSING.name());
        if (summary.getStartedAt() == null) {
            summary.setStartedAt(LocalDateTime.now());
        }
        summary.setUpdatedAt(LocalDateTime.now());
        dailySummaryMapper.updateById(summary);
    }

    private void rebuildTopUsers(TokenUsageMetricDailySummaryEntity summary,
                                 List<TokenUsageMetricDtos.TopUserUsage> users,
                                 long totalTokens) {
        topUserMapper.delete(new LambdaQueryWrapper<TokenUsageMetricTopUserEntity>()
                .eq(TokenUsageMetricTopUserEntity::getDailySummaryId, summary.getId()));
        int rank = 1;
        for (TokenUsageMetricDtos.TopUserUsage user : users) {
            TokenUsageMetricTopUserEntity entity = new TokenUsageMetricTopUserEntity();
            LocalDateTime now = LocalDateTime.now();
            long userTokens = nullToZero(user.getTokenUsage());
            entity.setDailySummaryId(summary.getId());
            entity.setTenantId(summary.getTenantId());
            entity.setMetricDate(summary.getMetricDate());
            entity.setUserId(user.getUserId());
            entity.setUsername(user.getUsername());
            entity.setDisplayName(user.getDisplayName());
            entity.setRankNo(rank++);
            entity.setTokenUsage(userTokens);
            entity.setMessageCount(nullToZero(user.getMessageCount()));
            entity.setUsageRatio(totalTokens <= 0 ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
                    : BigDecimal.valueOf(userTokens).divide(BigDecimal.valueOf(totalTokens), 4, RoundingMode.HALF_UP));
            entity.setMetricVersion(metricVersion());
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            topUserMapper.insert(entity);
        }
    }

    private boolean isDateAlreadyProcessed(Long tenantId, LocalDate metricDate) {
        TokenUsageMetricDailySummaryEntity summary = selectDailySummary(tenantId, metricDate);
        return summary != null && TokenUsageMetricDailyStatusEnum.COMPLETED.name().equals(summary.getStatus());
    }

    private TokenUsageMetricDailySummaryEntity selectDailySummary(Long tenantId, LocalDate metricDate) {
        return dailySummaryMapper.selectOne(new LambdaQueryWrapper<TokenUsageMetricDailySummaryEntity>()
                .eq(TokenUsageMetricDailySummaryEntity::getTenantId, tenantId)
                .eq(TokenUsageMetricDailySummaryEntity::getMetricDate, metricDate)
                .eq(TokenUsageMetricDailySummaryEntity::getMetricVersion, metricVersion())
                .last("LIMIT 1"));
    }

    private List<TokenUsageMetricDailySummaryEntity> selectSummaries(Long tenantId, LocalDate startDate, LocalDate endDate) {
        return dailySummaryMapper.selectList(summaryWrapper(tenantId, startDate, endDate)
                .orderByAsc(TokenUsageMetricDailySummaryEntity::getMetricDate));
    }

    private LambdaQueryWrapper<TokenUsageMetricDailySummaryEntity> summaryWrapper(Long tenantId, LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate == null ? LocalDate.now().minusDays(7) : startDate;
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        return new LambdaQueryWrapper<TokenUsageMetricDailySummaryEntity>()
                .eq(TokenUsageMetricDailySummaryEntity::getTenantId, tenantId)
                .eq(TokenUsageMetricDailySummaryEntity::getMetricVersion, metricVersion())
                .ge(TokenUsageMetricDailySummaryEntity::getMetricDate, start)
                .le(TokenUsageMetricDailySummaryEntity::getMetricDate, end);
    }

    private TokenUsageMetricDtos.TrendPoint toTrendPoint(TokenUsageMetricDailySummaryEntity entity) {
        TokenUsageMetricDtos.TrendPoint point = new TokenUsageMetricDtos.TrendPoint();
        point.setDate(entity.getMetricDate());
        point.setTotalTokenUsage(nullToZero(entity.getTotalTokenUsage()));
        point.setMessageCount(nullToZero(entity.getMessageCount()));
        point.setActiveUserCount(nullToZero(entity.getActiveUserCount()));
        point.setAverageTokensPerMessage(nullToZero(entity.getAverageTokensPerMessage(), 2));
        point.setStatus(entity.getStatus());
        point.setDailySummaryId(entity.getId());
        return point;
    }

    private TokenUsageMetricDtos.DailySummary toDailySummary(TokenUsageMetricDailySummaryEntity entity) {
        TokenUsageMetricDtos.DailySummary item = new TokenUsageMetricDtos.DailySummary();
        item.setId(entity.getId());
        item.setMetricDate(entity.getMetricDate());
        item.setMetricVersion(entity.getMetricVersion());
        item.setTotalTokenUsage(nullToZero(entity.getTotalTokenUsage()));
        item.setMessageCount(nullToZero(entity.getMessageCount()));
        item.setActiveUserCount(nullToZero(entity.getActiveUserCount()));
        item.setAverageTokensPerMessage(nullToZero(entity.getAverageTokensPerMessage(), 2));
        item.setTopUserLimit(entity.getTopUserLimit());
        item.setTopUserCount(entity.getTopUserCount());
        item.setStatus(entity.getStatus());
        item.setErrorMessage(entity.getErrorMessage());
        item.setStartedAt(entity.getStartedAt());
        item.setCompletedAt(entity.getCompletedAt());
        return item;
    }

    private TokenUsageMetricDtos.TopUser toTopUser(TokenUsageMetricTopUserEntity entity) {
        TokenUsageMetricDtos.TopUser item = new TokenUsageMetricDtos.TopUser();
        item.setId(entity.getId());
        item.setDailySummaryId(entity.getDailySummaryId());
        item.setMetricDate(entity.getMetricDate());
        item.setUserId(entity.getUserId());
        item.setUsername(entity.getUsername());
        item.setDisplayName(entity.getDisplayName());
        item.setRankNo(entity.getRankNo());
        item.setTokenUsage(nullToZero(entity.getTokenUsage()));
        item.setMessageCount(nullToZero(entity.getMessageCount()));
        item.setUsageRatio(nullToZero(entity.getUsageRatio(), 4));
        return item;
    }

    private LocalDate scheduledMetricDate() {
        return LocalDate.now().minusDays(Math.max(0, properties.getWorker().getMetricDateOffsetDays()));
    }

    private String metricVersion() {
        return properties.getVersion();
    }

    private BigDecimal averageTokens(long totalTokens, long messageCount) {
        if (messageCount <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(totalTokens).divide(BigDecimal.valueOf(messageCount), 2, RoundingMode.HALF_UP);
    }

    private long sumLong(List<TokenUsageMetricDailySummaryEntity> summaries,
                         java.util.function.Function<TokenUsageMetricDailySummaryEntity, Long> getter) {
        return summaries.stream().map(getter).filter(Objects::nonNull).mapToLong(Long::longValue).sum();
    }

    private long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    private BigDecimal nullToZero(BigDecimal value, int scale) {
        return value == null ? BigDecimal.ZERO.setScale(scale, RoundingMode.HALF_UP) : value;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
