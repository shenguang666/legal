package com.legal.retrieval.metrics.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.legal.chat.rag.RetrievedChunk;
import com.legal.common.AppException;
import com.legal.common.JsonUtils;
import com.legal.config.OpenAiChatModelProperties;
import com.legal.config.RagRetrievalMetricProperties;
import com.legal.retrieval.entity.RetrievalLogEntity;
import com.legal.retrieval.mapper.RetrievalLogMapper;
import com.legal.retrieval.metrics.dto.RagRetrievalMetricDtos;
import com.legal.retrieval.metrics.entity.RagRetrievalMetricDailySummaryEntity;
import com.legal.retrieval.metrics.entity.RagRetrievalMetricEvaluationEntity;
import com.legal.retrieval.metrics.enums.RagMetricDailyStatusEnum;
import com.legal.retrieval.metrics.enums.RagMetricEvaluationStatusEnum;
import com.legal.retrieval.metrics.enums.RagMetricScanStatusEnum;
import com.legal.retrieval.metrics.mapper.RagRetrievalMetricDailySummaryMapper;
import com.legal.retrieval.metrics.mapper.RagRetrievalMetricEvaluationMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class RagRetrievalMetricService {

    private final RagRetrievalMetricProperties properties;
    private final OpenAiChatModelProperties chatModelProperties;
    private final RetrievalLogMapper retrievalLogMapper;
    private final RagRetrievalMetricDailySummaryMapper dailySummaryMapper;
    private final RagRetrievalMetricEvaluationMapper evaluationMapper;
    private final RagMetricCandidateRecallService candidateRecallService;
    private final RagMetricLlmJudgeService judgeService;

    public RagRetrievalMetricService(RagRetrievalMetricProperties properties,
                                     OpenAiChatModelProperties chatModelProperties,
                                     RetrievalLogMapper retrievalLogMapper,
                                     RagRetrievalMetricDailySummaryMapper dailySummaryMapper,
                                     RagRetrievalMetricEvaluationMapper evaluationMapper,
                                     RagMetricCandidateRecallService candidateRecallService,
                                     RagMetricLlmJudgeService judgeService) {
        this.properties = properties;
        this.chatModelProperties = chatModelProperties;
        this.retrievalLogMapper = retrievalLogMapper;
        this.dailySummaryMapper = dailySummaryMapper;
        this.evaluationMapper = evaluationMapper;
        this.candidateRecallService = candidateRecallService;
        this.judgeService = judgeService;
    }

    public RagRetrievalMetricDtos.RunResult evaluateDate(LocalDate date) {
        LocalDate metricDate = date == null ? scheduledMetricDate() : date;
        return processDate(null, metricDate, false, false);
    }

    public RagRetrievalMetricDtos.RunResult evaluateScheduledRun() {
        LocalDate metricDate = scheduledMetricDate();
        RagRetrievalMetricDtos.RunResult result = hasProcessableLogs(null, metricDate)
                ? processDate(null, metricDate, false, false)
                : emptyRunResult(metricDate, false, "当前归档日期暂无待处�?RAG 消息");
        if (!hasProcessableLogs(null, metricDate)) {
            LocalDate backfillDate = findOlderProcessableDate(metricDate);
            if (backfillDate != null) {
                RagRetrievalMetricDtos.RunResult backfill = processDate(null, backfillDate, true, false);
                result.setSelected(result.getSelected() + backfill.getSelected());
                result.setSuccess(result.getSuccess() + backfill.getSuccess());
                result.setFailed(result.getFailed() + backfill.getFailed());
                result.setSkipped(result.getSkipped() + backfill.getSkipped());
                result.setBackfill(true);
                result.setMetricDate(backfillDate);
                result.setMessage("已处理当前归档日期，并补扫历史日�?" + backfillDate);
            }
        }
        return result;
    }

    public RagRetrievalMetricDtos.RunResult evaluateManualRange(Long tenantId, LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();
        LocalDate safeEnd = endDate == null ? startDate : endDate;
        LocalDate safeStart = startDate == null ? safeEnd : startDate;
        if (safeStart == null || safeEnd == null) {
            throw AppException.badRequest("请选择需要手动评估的历史日期");
        }
        if (safeEnd.isBefore(safeStart)) {
            throw AppException.badRequest("手动评估结束日期不能早于开始日期");
        }
        if (!safeEnd.isBefore(today)) {
            throw AppException.badRequest("手动评估结束日期只能选择今天之前的历史日期");
        }
        RagRetrievalMetricDtos.RunResult merged = new RagRetrievalMetricDtos.RunResult();
        merged.setStartDate(safeStart);
        merged.setEndDate(safeEnd);
        int alreadyProcessedDates = 0;
        int noPendingDates = 0;
        LocalDate cursor = safeStart;
        while (!cursor.isAfter(safeEnd)) {
            if (isDateAlreadyProcessed(tenantId, cursor)) {
                alreadyProcessedDates++;
                cursor = cursor.plusDays(1);
                continue;
            }
            if (!hasProcessableLogs(tenantId, cursor)) {
                noPendingDates++;
                cursor = cursor.plusDays(1);
                continue;
            }
            RagRetrievalMetricDtos.RunResult current = processDate(tenantId, cursor, false, true);
            merged.setSelected(merged.getSelected() + current.getSelected());
            merged.setSuccess(merged.getSuccess() + current.getSuccess());
            merged.setFailed(merged.getFailed() + current.getFailed());
            merged.setSkipped(merged.getSkipped() + current.getSkipped());
            cursor = cursor.plusDays(1);
        }
        merged.setAlreadyProcessedDates(alreadyProcessedDates);
        merged.setAlreadyProcessed(alreadyProcessedDates > 0 && merged.getSelected() == 0);
        if (merged.isAlreadyProcessed()) {
            merged.setMessage("已有记录，无需评估");
            merged.setSkippedReason("already-processed");
        } else if (merged.getSelected() == 0 && noPendingDates > 0) {
            merged.setMessage("暂无待处理 RAG 消息，无需评估");
            merged.setSkippedReason("no-pending-logs");
        } else if (alreadyProcessedDates > 0) {
            merged.setMessage("部分日期已有记录，已跳过；其余日期评估完成");
        } else {
            merged.setMessage("手动评估完成");
        }
        return merged;
    }

    private RagRetrievalMetricDtos.RunResult processDate(Long tenantId, LocalDate metricDate, boolean backfill, boolean manual) {
        List<RetrievalLogEntity> logs = selectBatchLogs(tenantId, metricDate);
        RagRetrievalMetricDtos.RunResult result = new RagRetrievalMetricDtos.RunResult();
        result.setMetricDate(metricDate);
        result.setBackfill(backfill);
        result.setSelected(logs.size());
        int success = 0;
        int failed = 0;
        int skipped = 0;
        for (RetrievalLogEntity log : logs) {
            LocalDate logMetricDate = log.getCreatedAt() == null ? metricDate : log.getCreatedAt().toLocalDate();
            RagRetrievalMetricDailySummaryEntity summary = getOrCreateDailySummary(log.getTenantId(), logMetricDate);
            String previousScanStatus = log.getRagMetricScanStatus();
            markLogProcessing(log, summary.getId());
            resetRetryEvaluation(log, previousScanStatus);
            RagRetrievalMetricEvaluationEntity evaluation = evaluateOne(log, summary.getId());
            RagMetricEvaluationStatusEnum status = RagMetricEvaluationStatusEnum.valueOf(evaluation.getStatus());
            if (status == RagMetricEvaluationStatusEnum.SUCCESS) {
                success++;
                markLogTerminal(log, summary.getId(), RagMetricScanStatusEnum.SUCCESS, null, false);
            } else if (status == RagMetricEvaluationStatusEnum.SKIPPED) {
                skipped++;
                markLogTerminal(log, summary.getId(), RagMetricScanStatusEnum.FILTERED, evaluation.getErrorMessage(), false);
            } else {
                failed++;
                markLogTerminal(log, summary.getId(), RagMetricScanStatusEnum.FAILED, evaluation.getErrorMessage(), true);
            }
            refreshDailySummary(summary.getId());
        }
        completeSummaries(metricDate, tenantId);
        result.setSuccess(success);
        result.setFailed(failed);
        result.setSkipped(skipped);
        if (!manual && logs.isEmpty()) {
            result.setSkippedReason("no-pending-logs");
        }
        return result;
    }

    @Transactional
    public RagRetrievalMetricEvaluationEntity evaluateOne(RetrievalLogEntity log) {
        return evaluateOne(log, null);
    }

    public RagRetrievalMetricDtos.Summary summary(Long tenantId, LocalDate startDate, LocalDate endDate) {
        List<RagRetrievalMetricDailySummaryEntity> summaries = selectSummaries(tenantId, startDate, endDate);
        List<RagRetrievalMetricDailySummaryEntity> completed = summaries.stream()
                .filter(item -> !RagMetricDailyStatusEnum.FAILED.name().equals(item.getStatus()))
                .toList();
        RagRetrievalMetricDtos.Summary summary = new RagRetrievalMetricDtos.Summary();
        summary.setEvaluatedCount(sumLong(summaries, RagRetrievalMetricDailySummaryEntity::getTotalRagMessageCount));
        summary.setValidMessageCount(sumLong(summaries, RagRetrievalMetricDailySummaryEntity::getValidMessageCount));
        summary.setFilteredCount(sumLong(summaries, RagRetrievalMetricDailySummaryEntity::getFilteredMessageCount));
        summary.setSuccessCount(sumLong(summaries, RagRetrievalMetricDailySummaryEntity::getSuccessCount));
        summary.setFailedCount(sumLong(summaries, RagRetrievalMetricDailySummaryEntity::getFailedCount));
        summary.setSkippedCount(sumLong(summaries, RagRetrievalMetricDailySummaryEntity::getSkippedCount));
        summary.setCandidateTopN(properties.getCandidateRecall().getTopN());
        summary.setEvaluationVersion(evaluationVersion());
        summary.setAverageRecall(weightedAverage(completed, true));
        summary.setAveragePrecision(weightedAverage(completed, false));
        summary.setTrends(summaries.stream().map(this::toTrendPoint).toList());
        summary.setDailySummaries(summaries.stream().map(this::toDailySummary).toList());
        return summary;
    }

    public RagRetrievalMetricDtos.DailySummaryPage dailySummaries(Long tenantId, LocalDate startDate, LocalDate endDate, int pageNo, int pageSize) {
        int safePageNo = Math.max(1, pageNo);
        int safePageSize = Math.max(1, Math.min(pageSize, 100));
        LambdaQueryWrapper<RagRetrievalMetricDailySummaryEntity> wrapper = summaryWrapper(tenantId, startDate, endDate)
                .orderByDesc(RagRetrievalMetricDailySummaryEntity::getMetricDate);
        Page<RagRetrievalMetricDailySummaryEntity> page = dailySummaryMapper.selectPage(Page.of(safePageNo, safePageSize), wrapper);
        RagRetrievalMetricDtos.DailySummaryPage result = new RagRetrievalMetricDtos.DailySummaryPage();
        result.setTotal(page.getTotal());
        result.setPageNo(safePageNo);
        result.setPageSize(safePageSize);
        result.setItems(page.getRecords().stream().map(this::toDailySummary).toList());
        return result;
    }

    public RagRetrievalMetricDtos.DetailPage details(Long tenantId, LocalDate startDate, LocalDate endDate, Long dailySummaryId, int pageNo, int pageSize) {
        int safePageNo = Math.max(1, pageNo);
        int safePageSize = Math.max(1, Math.min(pageSize, 100));
        LambdaQueryWrapper<RagRetrievalMetricEvaluationEntity> wrapper = detailWrapper(tenantId, startDate, endDate, dailySummaryId)
                .orderByDesc(RagRetrievalMetricEvaluationEntity::getEvaluatedAt);
        Page<RagRetrievalMetricEvaluationEntity> page = evaluationMapper.selectPage(Page.of(safePageNo, safePageSize), wrapper);
        RagRetrievalMetricDtos.DetailPage result = new RagRetrievalMetricDtos.DetailPage();
        result.setTotal(page.getTotal());
        result.setPageNo(safePageNo);
        result.setPageSize(safePageSize);
        result.setItems(page.getRecords().stream().map(this::toDetail).toList());
        return result;
    }

    public RagRetrievalMetricDtos.DetailPage details(Long tenantId, LocalDate startDate, LocalDate endDate, int pageNo, int pageSize) {
        return details(tenantId, startDate, endDate, null, pageNo, pageSize);
    }

    private RagRetrievalMetricEvaluationEntity evaluateOne(RetrievalLogEntity log, Long dailySummaryId) {
        RagRetrievalMetricEvaluationEntity entity = baseEntity(log, dailySummaryId);
        try {
            List<Long> originalHitIds = parseChunkIds(log.getHitChunkIds());
            if (!isEvaluableQuery(log.getQueryText())) {
                fillSkipped(entity, originalHitIds);
                persist(entity);
                return entity;
            }
            int topN = Math.max(1, properties.getCandidateRecall().getTopN());
            List<RetrievedChunk> candidates = candidateRecallService.recall(log.getTenantId(), log.getQueryText(), topN);
            List<RetrievedChunk> judgeCandidates = selectJudgeCandidates(candidates, originalHitIds);
            RagMetricJudgeResult judgeResult = judgeService.judge(log.getQueryText(), originalHitIds, judgeCandidates);
            if (!judgeResult.success()) {
                fillFailed(entity, originalHitIds, candidates.size(), topN, judgeResult);
                persist(entity);
                return entity;
            }
            fillSuccess(entity, originalHitIds, candidates.size(), topN, judgeResult);
        } catch (Exception ex) {
            entity.setStatus(RagMetricEvaluationStatusEnum.FAILED.name());
            entity.setErrorMessage(truncate(ex.getMessage(), 1000));
        }
        persist(entity);
        return entity;
    }

    private List<RetrievalLogEntity> selectBatchLogs(Long tenantId, LocalDate metricDate) {
        int batchSize = Math.max(1, properties.getWorker().getBatchSize());
        LocalDateTime start = metricDate.atStartOfDay();
        LocalDateTime end = metricDate.atTime(LocalTime.MAX);
        LambdaQueryWrapper<RetrievalLogEntity> wrapper = new LambdaQueryWrapper<RetrievalLogEntity>()
                .ge(RetrievalLogEntity::getCreatedAt, start)
                .le(RetrievalLogEntity::getCreatedAt, end)
                .isNotNull(RetrievalLogEntity::getHitChunkIds)
                .and(this::processableStatusCondition)
                .orderByAsc(RetrievalLogEntity::getId)
                .last("LIMIT " + batchSize);
        if (tenantId != null) {
            wrapper.eq(RetrievalLogEntity::getTenantId, tenantId);
        }
        return retrievalLogMapper.selectList(wrapper);
    }

    private RagRetrievalMetricDailySummaryEntity getOrCreateDailySummary(Long tenantId, LocalDate metricDate) {
        RagRetrievalMetricDailySummaryEntity existing = selectDailySummary(tenantId, metricDate);
        if (existing != null) {
            return existing;
        }
        LocalDateTime now = LocalDateTime.now();
        RagRetrievalMetricDailySummaryEntity created = new RagRetrievalMetricDailySummaryEntity();
        created.setTenantId(tenantId);
        created.setMetricDate(metricDate);
        created.setEvaluationVersion(evaluationVersion());
        created.setCandidateTopN(properties.getCandidateRecall().getTopN());
        created.setStatus(RagMetricDailyStatusEnum.PROCESSING.name());
        created.setTotalRagMessageCount(0L);
        created.setValidMessageCount(0L);
        created.setFilteredMessageCount(0L);
        created.setSuccessCount(0L);
        created.setFailedCount(0L);
        created.setSkippedCount(0L);
        created.setAverageRecall(toScore(0D));
        created.setAveragePrecision(toScore(0D));
        created.setStartedAt(now);
        created.setCreatedAt(now);
        created.setUpdatedAt(now);
        try {
            dailySummaryMapper.insert(created);
        } catch (DuplicateKeyException ex) {
            RagRetrievalMetricDailySummaryEntity concurrent = selectDailySummary(tenantId, metricDate);
            if (concurrent != null) {
                return concurrent;
            }
            throw ex;
        }
        return created;
    }

    private RagRetrievalMetricDailySummaryEntity selectDailySummary(Long tenantId, LocalDate metricDate) {
        return dailySummaryMapper.selectOne(new LambdaQueryWrapper<RagRetrievalMetricDailySummaryEntity>()
                .eq(RagRetrievalMetricDailySummaryEntity::getTenantId, tenantId)
                .eq(RagRetrievalMetricDailySummaryEntity::getMetricDate, metricDate)
                .eq(RagRetrievalMetricDailySummaryEntity::getEvaluationVersion, evaluationVersion())
                .last("LIMIT 1"));
    }

    private void refreshDailySummary(Long summaryId) {
        RagRetrievalMetricDailySummaryEntity summary = dailySummaryMapper.selectById(summaryId);
        if (summary == null) {
            return;
        }
        LocalDateTime start = summary.getMetricDate().atStartOfDay();
        LocalDateTime end = summary.getMetricDate().atTime(LocalTime.MAX);
        long total = countLogs(summary.getTenantId(), start, end, null);
        long filtered = countLogs(summary.getTenantId(), start, end, RagMetricScanStatusEnum.FILTERED);
        long success = countEvaluations(summaryId, RagMetricEvaluationStatusEnum.SUCCESS);
        long failed = countEvaluations(summaryId, RagMetricEvaluationStatusEnum.FAILED);
        long skipped = countEvaluations(summaryId, RagMetricEvaluationStatusEnum.SKIPPED);
        List<RagRetrievalMetricEvaluationEntity> successes = evaluationMapper.selectList(new LambdaQueryWrapper<RagRetrievalMetricEvaluationEntity>()
                .eq(RagRetrievalMetricEvaluationEntity::getDailySummaryId, summaryId)
                .eq(RagRetrievalMetricEvaluationEntity::getStatus, RagMetricEvaluationStatusEnum.SUCCESS.name()));
        summary.setTotalRagMessageCount(total);
        summary.setValidMessageCount(success + failed);
        summary.setFilteredMessageCount(filtered);
        summary.setSuccessCount(success);
        summary.setFailedCount(failed);
        summary.setSkippedCount(skipped);
        summary.setAverageRecall(average(successes.stream().map(RagRetrievalMetricEvaluationEntity::getRecallScore).toList()));
        summary.setAveragePrecision(average(successes.stream().map(RagRetrievalMetricEvaluationEntity::getPrecisionScore).toList()));
        summary.setUpdatedAt(LocalDateTime.now());
        dailySummaryMapper.updateById(summary);
    }

    private void completeSummaries(LocalDate metricDate, Long tenantId) {
        LambdaQueryWrapper<RagRetrievalMetricDailySummaryEntity> wrapper = new LambdaQueryWrapper<RagRetrievalMetricDailySummaryEntity>()
                .eq(RagRetrievalMetricDailySummaryEntity::getMetricDate, metricDate)
                .eq(RagRetrievalMetricDailySummaryEntity::getEvaluationVersion, evaluationVersion());
        if (tenantId != null) {
            wrapper.eq(RagRetrievalMetricDailySummaryEntity::getTenantId, tenantId);
        }
        List<RagRetrievalMetricDailySummaryEntity> summaries = dailySummaryMapper.selectList(wrapper);
        for (RagRetrievalMetricDailySummaryEntity summary : summaries) {
            if (hasRemainingLogs(summary)) {
                continue;
            }
            refreshDailySummary(summary.getId());
            RagRetrievalMetricDailySummaryEntity refreshed = dailySummaryMapper.selectById(summary.getId());
            refreshed.setStatus(refreshed.getFailedCount() != null && refreshed.getFailedCount() > 0
                    ? RagMetricDailyStatusEnum.PARTIAL_FAILED.name()
                    : RagMetricDailyStatusEnum.COMPLETED.name());
            refreshed.setCompletedAt(LocalDateTime.now());
            refreshed.setUpdatedAt(LocalDateTime.now());
            dailySummaryMapper.updateById(refreshed);
        }
    }

    private boolean hasRemainingLogs(RagRetrievalMetricDailySummaryEntity summary) {
        LocalDateTime start = summary.getMetricDate().atStartOfDay();
        LocalDateTime end = summary.getMetricDate().atTime(LocalTime.MAX);
        Long count = retrievalLogMapper.selectCount(new LambdaQueryWrapper<RetrievalLogEntity>()
                .eq(RetrievalLogEntity::getTenantId, summary.getTenantId())
                .ge(RetrievalLogEntity::getCreatedAt, start)
                .le(RetrievalLogEntity::getCreatedAt, end)
                .isNotNull(RetrievalLogEntity::getHitChunkIds)
                .and(this::remainingStatusCondition));
        return count != null && count > 0;
    }

    private boolean hasProcessableLogs(Long tenantId, LocalDate metricDate) {
        LocalDateTime start = metricDate.atStartOfDay();
        LocalDateTime end = metricDate.atTime(LocalTime.MAX);
        LambdaQueryWrapper<RetrievalLogEntity> wrapper = new LambdaQueryWrapper<RetrievalLogEntity>()
                .ge(RetrievalLogEntity::getCreatedAt, start)
                .le(RetrievalLogEntity::getCreatedAt, end)
                .isNotNull(RetrievalLogEntity::getHitChunkIds)
                .and(this::processableStatusCondition);
        if (tenantId != null) {
            wrapper.eq(RetrievalLogEntity::getTenantId, tenantId);
        }
        Long count = retrievalLogMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    private boolean hasRemainingLogs(Long tenantId, LocalDate metricDate) {
        LocalDateTime start = metricDate.atStartOfDay();
        LocalDateTime end = metricDate.atTime(LocalTime.MAX);
        LambdaQueryWrapper<RetrievalLogEntity> wrapper = new LambdaQueryWrapper<RetrievalLogEntity>()
                .ge(RetrievalLogEntity::getCreatedAt, start)
                .le(RetrievalLogEntity::getCreatedAt, end)
                .isNotNull(RetrievalLogEntity::getHitChunkIds)
                .and(this::remainingStatusCondition);
        if (tenantId != null) {
            wrapper.eq(RetrievalLogEntity::getTenantId, tenantId);
        }
        Long count = retrievalLogMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    private LocalDate findOlderProcessableDate(LocalDate beforeDate) {
        List<RetrievalLogEntity> logs = retrievalLogMapper.selectList(new LambdaQueryWrapper<RetrievalLogEntity>()
                .lt(RetrievalLogEntity::getCreatedAt, beforeDate.atStartOfDay())
                .isNotNull(RetrievalLogEntity::getHitChunkIds)
                .and(this::processableStatusCondition)
                .orderByAsc(RetrievalLogEntity::getCreatedAt)
                .orderByAsc(RetrievalLogEntity::getId)
                .last("LIMIT 1"));
        if (logs.isEmpty() || logs.get(0).getCreatedAt() == null) {
            return null;
        }
        return logs.get(0).getCreatedAt().toLocalDate();
    }

    private void processableStatusCondition(LambdaQueryWrapper<RetrievalLogEntity> wrapper) {
        LocalDateTime staleProcessingBefore = LocalDateTime.now().minus(properties.getWorker().getTimeout());
        wrapper.isNull(RetrievalLogEntity::getRagMetricScanStatus)
                .or()
                .eq(RetrievalLogEntity::getRagMetricScanStatus, RagMetricScanStatusEnum.PENDING.name())
                .or(nested -> nested
                        .eq(RetrievalLogEntity::getRagMetricScanStatus, RagMetricScanStatusEnum.FAILED.name())
                        .and(retry -> retry
                                .isNull(RetrievalLogEntity::getRagMetricRetryCount)
                                .or()
                                .lt(RetrievalLogEntity::getRagMetricRetryCount, properties.getWorker().getMaxRetries())))
                .or(nested -> nested
                        .eq(RetrievalLogEntity::getRagMetricScanStatus, RagMetricScanStatusEnum.PROCESSING.name())
                        .and(stale -> stale
                                .isNull(RetrievalLogEntity::getRagMetricScannedAt)
                                .or()
                                .lt(RetrievalLogEntity::getRagMetricScannedAt, staleProcessingBefore)));
    }

    private void remainingStatusCondition(LambdaQueryWrapper<RetrievalLogEntity> wrapper) {
        wrapper.isNull(RetrievalLogEntity::getRagMetricScanStatus)
                .or()
                .eq(RetrievalLogEntity::getRagMetricScanStatus, RagMetricScanStatusEnum.PENDING.name())
                .or()
                .eq(RetrievalLogEntity::getRagMetricScanStatus, RagMetricScanStatusEnum.PROCESSING.name())
                .or(nested -> nested
                        .eq(RetrievalLogEntity::getRagMetricScanStatus, RagMetricScanStatusEnum.FAILED.name())
                        .and(retry -> retry
                                .isNull(RetrievalLogEntity::getRagMetricRetryCount)
                                .or()
                                .lt(RetrievalLogEntity::getRagMetricRetryCount, properties.getWorker().getMaxRetries())));
    }

    private boolean isDateAlreadyProcessed(Long tenantId, LocalDate metricDate) {
        if (hasRemainingLogs(tenantId, metricDate)) {
            return false;
        }
        return selectDailySummary(tenantId, metricDate) != null || hasTerminalMetricLogs(tenantId, metricDate);
    }

    private boolean hasTerminalMetricLogs(Long tenantId, LocalDate metricDate) {
        LocalDateTime start = metricDate.atStartOfDay();
        LocalDateTime end = metricDate.atTime(LocalTime.MAX);
        Long count = retrievalLogMapper.selectCount(new LambdaQueryWrapper<RetrievalLogEntity>()
                .eq(RetrievalLogEntity::getTenantId, tenantId)
                .ge(RetrievalLogEntity::getCreatedAt, start)
                .le(RetrievalLogEntity::getCreatedAt, end)
                .isNotNull(RetrievalLogEntity::getHitChunkIds)
                .and(wrapper -> wrapper
                        .in(RetrievalLogEntity::getRagMetricScanStatus,
                                RagMetricScanStatusEnum.SUCCESS.name(),
                                RagMetricScanStatusEnum.FILTERED.name())
                        .or(nested -> nested
                                .eq(RetrievalLogEntity::getRagMetricScanStatus, RagMetricScanStatusEnum.FAILED.name())
                                .ge(RetrievalLogEntity::getRagMetricRetryCount, properties.getWorker().getMaxRetries()))));
        return count != null && count > 0;
    }

    private RagRetrievalMetricDtos.RunResult emptyRunResult(LocalDate metricDate, boolean backfill, String message) {
        RagRetrievalMetricDtos.RunResult result = new RagRetrievalMetricDtos.RunResult();
        result.setMetricDate(metricDate);
        result.setBackfill(backfill);
        result.setMessage(message);
        result.setSkippedReason("no-pending-logs");
        return result;
    }

    private void markLogProcessing(RetrievalLogEntity log, Long summaryId) {
        log.setRagMetricScanStatus(RagMetricScanStatusEnum.PROCESSING.name());
        log.setRagMetricScannedAt(LocalDateTime.now());
        log.setRagMetricSummaryId(summaryId);
        log.setRagMetricErrorMessage(null);
        retrievalLogMapper.updateById(log);
    }

    private void resetRetryEvaluation(RetrievalLogEntity log, String previousScanStatus) {
        if (!RagMetricScanStatusEnum.FAILED.name().equals(previousScanStatus)) {
            return;
        }
        evaluationMapper.delete(new LambdaQueryWrapper<RagRetrievalMetricEvaluationEntity>()
                .eq(RagRetrievalMetricEvaluationEntity::getRetrievalLogId, log.getId())
                .eq(RagRetrievalMetricEvaluationEntity::getEvaluationVersion, evaluationVersion())
                .eq(RagRetrievalMetricEvaluationEntity::getStatus, RagMetricEvaluationStatusEnum.FAILED.name()));
    }

    private void markLogTerminal(RetrievalLogEntity log, Long summaryId, RagMetricScanStatusEnum status, String errorMessage, boolean increaseRetry) {
        log.setRagMetricScanStatus(status.name());
        log.setRagMetricSummaryId(summaryId);
        log.setRagMetricScannedAt(LocalDateTime.now());
        log.setRagMetricErrorMessage(truncate(errorMessage, 1000));
        if (increaseRetry) {
            log.setRagMetricRetryCount((log.getRagMetricRetryCount() == null ? 0 : log.getRagMetricRetryCount()) + 1);
        }
        retrievalLogMapper.updateById(log);
    }

    private long countLogs(Long tenantId, LocalDateTime start, LocalDateTime end, RagMetricScanStatusEnum status) {
        LambdaQueryWrapper<RetrievalLogEntity> wrapper = new LambdaQueryWrapper<RetrievalLogEntity>()
                .eq(RetrievalLogEntity::getTenantId, tenantId)
                .ge(RetrievalLogEntity::getCreatedAt, start)
                .le(RetrievalLogEntity::getCreatedAt, end)
                .isNotNull(RetrievalLogEntity::getHitChunkIds);
        if (status != null) {
            wrapper.eq(RetrievalLogEntity::getRagMetricScanStatus, status.name());
        }
        Long count = retrievalLogMapper.selectCount(wrapper);
        return count == null ? 0L : count;
    }

    private long countEvaluations(Long summaryId, RagMetricEvaluationStatusEnum status) {
        Long count = evaluationMapper.selectCount(new LambdaQueryWrapper<RagRetrievalMetricEvaluationEntity>()
                .eq(RagRetrievalMetricEvaluationEntity::getDailySummaryId, summaryId)
                .eq(RagRetrievalMetricEvaluationEntity::getStatus, status.name()));
        return count == null ? 0L : count;
    }

    private void persist(RagRetrievalMetricEvaluationEntity entity) {
        LocalDateTime now = LocalDateTime.now();
        entity.setEvaluatedAt(now);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        evaluationMapper.insert(entity);
    }

    private RagRetrievalMetricEvaluationEntity baseEntity(RetrievalLogEntity log, Long dailySummaryId) {
        RagRetrievalMetricEvaluationEntity entity = new RagRetrievalMetricEvaluationEntity();
        entity.setDailySummaryId(dailySummaryId);
        entity.setRetrievalLogId(log.getId());
        entity.setTenantId(log.getTenantId());
        entity.setQueryText(log.getQueryText());
        entity.setModelName(StringUtils.hasText(properties.getJudgeModel().getModelName())
                ? properties.getJudgeModel().getModelName()
                : chatModelProperties.getModelName());
        entity.setPromptVersion(properties.getJudgeModel().getPromptVersion());
        entity.setEvaluationVersion(evaluationVersion());
        return entity;
    }

    private void fillSkipped(RagRetrievalMetricEvaluationEntity entity, List<Long> originalHitIds) {
        entity.setOriginalHitChunkIds(JsonUtils.toJson(originalHitIds));
        entity.setTruePositive(0);
        entity.setFalseNegative(0);
        entity.setFalsePositive(0);
        entity.setRecallScore(toScore(0D));
        entity.setPrecisionScore(toScore(0D));
        entity.setEvaluatedCandidateCount(0);
        entity.setEvaluatedTopN(properties.getCandidateRecall().getTopN());
        entity.setJudgeSummary("query-filtered");
        entity.setExplanation("Query 过短、寒暄或代表性不足，已跳过评估");
        entity.setStatus(RagMetricEvaluationStatusEnum.SKIPPED.name());
        entity.setErrorMessage("low-quality-query");
    }

    private void fillFailed(RagRetrievalMetricEvaluationEntity entity, List<Long> originalHitIds, int candidateCount, int topN, RagMetricJudgeResult judgeResult) {
        entity.setOriginalHitChunkIds(JsonUtils.toJson(originalHitIds));
        entity.setTruePositive(0);
        entity.setFalseNegative(0);
        entity.setFalsePositive(0);
        entity.setRecallScore(toScore(0D));
        entity.setPrecisionScore(toScore(0D));
        entity.setEvaluatedCandidateCount(candidateCount);
        entity.setEvaluatedTopN(topN);
        entity.setJudgeSummary(judgeResult.rawSummary());
        entity.setExplanation(judgeResult.explanation());
        entity.setStatus(RagMetricEvaluationStatusEnum.FAILED.name());
        entity.setErrorMessage(truncate(judgeResult.failureReason(), 1000));
    }

    private void fillSuccess(RagRetrievalMetricEvaluationEntity entity, List<Long> originalHitIds, int candidateCount, int topN, RagMetricJudgeResult judgeResult) {
        int tp = judgeResult.relevantOriginalChunkIds().size();
        int fn = judgeResult.missedRelevantChunkIds().size();
        Set<Long> relevantSet = Set.copyOf(judgeResult.relevantOriginalChunkIds());
        int fp = judgeResult.irrelevantOriginalChunkIds().isEmpty()
                ? (int) originalHitIds.stream().filter(id -> !relevantSet.contains(id)).count()
                : judgeResult.irrelevantOriginalChunkIds().size();
        entity.setOriginalHitChunkIds(JsonUtils.toJson(originalHitIds));
        entity.setRelevantOriginalChunkIds(JsonUtils.toJson(judgeResult.relevantOriginalChunkIds()));
        entity.setIrrelevantOriginalChunkIds(JsonUtils.toJson(judgeResult.irrelevantOriginalChunkIds()));
        entity.setMissedRelevantChunkIds(JsonUtils.toJson(judgeResult.missedRelevantChunkIds()));
        entity.setTruePositive(tp);
        entity.setFalseNegative(fn);
        entity.setFalsePositive(fp);
        entity.setRecallScore(toScore(RagRetrievalMetricCalculator.recall(tp, fn)));
        entity.setPrecisionScore(toScore(RagRetrievalMetricCalculator.precision(tp, fp)));
        entity.setEvaluatedCandidateCount(candidateCount);
        entity.setEvaluatedTopN(topN);
        entity.setJudgeSummary(judgeResult.rawSummary());
        entity.setExplanation(judgeResult.explanation());
        entity.setStatus(RagMetricEvaluationStatusEnum.SUCCESS.name());
    }

    private String evaluationVersion() {
        return properties.getJudgeModel().getPromptVersion()
                + ":top" + properties.getCandidateRecall().getTopN()
                + ":judge" + judgeCandidateLimit(properties.getCandidateRecall().getTopN());
    }

    private LocalDate scheduledMetricDate() {
        return LocalDate.now().minusDays(Math.max(0, properties.getWorker().getMetricDateOffsetDays()));
    }

    private List<RetrievedChunk> selectJudgeCandidates(List<RetrievedChunk> candidates, List<Long> originalHitIds) {
        int limit = judgeCandidateLimit(candidates.size());
        List<RetrievedChunk> selected = new ArrayList<>();
        Set<Long> selectedIds = new java.util.LinkedHashSet<>();
        Set<Long> originalIdSet = Set.copyOf(originalHitIds);
        for (RetrievedChunk candidate : candidates) {
            if (originalIdSet.contains(candidate.getChunkId()) && selectedIds.add(candidate.getChunkId())) {
                selected.add(candidate);
            }
            if (selected.size() >= limit) {
                return selected;
            }
        }
        for (RetrievedChunk candidate : candidates) {
            if (selectedIds.add(candidate.getChunkId())) {
                selected.add(candidate);
            }
            if (selected.size() >= limit) {
                return selected;
            }
        }
        return selected;
    }

    private int judgeCandidateLimit(int candidateCount) {
        if (candidateCount <= 0) {
            return 0;
        }
        RagRetrievalMetricProperties.CandidateRecall recall = properties.getCandidateRecall();
        double ratio = Math.max(0.01D, Math.min(1D, recall.getJudgeCandidateRatio()));
        int ratioCount = (int) Math.ceil(candidateCount * ratio);
        int min = Math.max(1, recall.getMinJudgeCandidates());
        int max = Math.max(min, recall.getMaxJudgeCandidates());
        return Math.min(candidateCount, Math.min(max, Math.max(min, ratioCount)));
    }

    private boolean isEvaluableQuery(String queryText) {
        if (!StringUtils.hasText(queryText)) {
            return false;
        }
        String normalized = queryText.replaceAll("\\s+", "").toLowerCase();
        if (normalized.length() < Math.max(1, properties.getQueryFilter().getMinQueryLength())) {
            return false;
        }
        return properties.getQueryFilter().getExcludedQueries().stream()
                .filter(StringUtils::hasText)
                .map(item -> item.replaceAll("\\s+", "").toLowerCase())
                .noneMatch(normalized::equals);
    }

    private List<RagRetrievalMetricDailySummaryEntity> selectSummaries(Long tenantId, LocalDate startDate, LocalDate endDate) {
        return dailySummaryMapper.selectList(summaryWrapper(tenantId, startDate, endDate)
                .orderByAsc(RagRetrievalMetricDailySummaryEntity::getMetricDate));
    }

    private LambdaQueryWrapper<RagRetrievalMetricDailySummaryEntity> summaryWrapper(Long tenantId, LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate == null ? LocalDate.now().minusDays(7) : startDate;
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        return new LambdaQueryWrapper<RagRetrievalMetricDailySummaryEntity>()
                .eq(RagRetrievalMetricDailySummaryEntity::getTenantId, tenantId)
                .eq(RagRetrievalMetricDailySummaryEntity::getEvaluationVersion, evaluationVersion())
                .ge(RagRetrievalMetricDailySummaryEntity::getMetricDate, start)
                .le(RagRetrievalMetricDailySummaryEntity::getMetricDate, end);
    }

    private LambdaQueryWrapper<RagRetrievalMetricEvaluationEntity> detailWrapper(Long tenantId, LocalDate startDate, LocalDate endDate, Long dailySummaryId) {
        LocalDate start = startDate == null ? LocalDate.now().minusDays(7) : startDate;
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        LambdaQueryWrapper<RagRetrievalMetricEvaluationEntity> wrapper = new LambdaQueryWrapper<RagRetrievalMetricEvaluationEntity>()
                .eq(RagRetrievalMetricEvaluationEntity::getTenantId, tenantId)
                .eq(RagRetrievalMetricEvaluationEntity::getEvaluationVersion, evaluationVersion());
        if (dailySummaryId != null) {
            wrapper.eq(RagRetrievalMetricEvaluationEntity::getDailySummaryId, dailySummaryId);
        } else {
            wrapper.ge(RagRetrievalMetricEvaluationEntity::getEvaluatedAt, start.atStartOfDay())
                    .le(RagRetrievalMetricEvaluationEntity::getEvaluatedAt, end.atTime(LocalTime.MAX));
        }
        return wrapper;
    }

    private RagRetrievalMetricDtos.TrendPoint toTrendPoint(RagRetrievalMetricDailySummaryEntity summary) {
        RagRetrievalMetricDtos.TrendPoint point = new RagRetrievalMetricDtos.TrendPoint();
        point.setDate(summary.getMetricDate());
        point.setAverageRecall(nullToZero(summary.getAverageRecall()));
        point.setAveragePrecision(nullToZero(summary.getAveragePrecision()));
        point.setTotalRagMessageCount(nullToZero(summary.getTotalRagMessageCount()));
        point.setValidMessageCount(nullToZero(summary.getValidMessageCount()));
        point.setSuccessCount(nullToZero(summary.getSuccessCount()));
        point.setFailedCount(nullToZero(summary.getFailedCount()));
        point.setSkippedCount(nullToZero(summary.getSkippedCount()));
        point.setFilteredCount(nullToZero(summary.getFilteredMessageCount()));
        point.setStatus(summary.getStatus());
        point.setDailySummaryId(summary.getId());
        return point;
    }

    private RagRetrievalMetricDtos.DailySummary toDailySummary(RagRetrievalMetricDailySummaryEntity entity) {
        RagRetrievalMetricDtos.DailySummary item = new RagRetrievalMetricDtos.DailySummary();
        item.setId(entity.getId());
        item.setMetricDate(entity.getMetricDate());
        item.setEvaluationVersion(entity.getEvaluationVersion());
        item.setTotalRagMessageCount(nullToZero(entity.getTotalRagMessageCount()));
        item.setValidMessageCount(nullToZero(entity.getValidMessageCount()));
        item.setFilteredMessageCount(nullToZero(entity.getFilteredMessageCount()));
        item.setSuccessCount(nullToZero(entity.getSuccessCount()));
        item.setFailedCount(nullToZero(entity.getFailedCount()));
        item.setSkippedCount(nullToZero(entity.getSkippedCount()));
        item.setAverageRecall(nullToZero(entity.getAverageRecall()));
        item.setAveragePrecision(nullToZero(entity.getAveragePrecision()));
        item.setCandidateTopN(entity.getCandidateTopN());
        item.setStatus(entity.getStatus());
        item.setErrorMessage(entity.getErrorMessage());
        item.setStartedAt(entity.getStartedAt());
        item.setCompletedAt(entity.getCompletedAt());
        return item;
    }

    private RagRetrievalMetricDtos.Detail toDetail(RagRetrievalMetricEvaluationEntity entity) {
        RagRetrievalMetricDtos.Detail detail = new RagRetrievalMetricDtos.Detail();
        detail.setId(entity.getId());
        detail.setDailySummaryId(entity.getDailySummaryId());
        detail.setRetrievalLogId(entity.getRetrievalLogId());
        detail.setQueryText(entity.getQueryText());
        detail.setOriginalHitChunkIds(jsonIds(entity.getOriginalHitChunkIds()));
        detail.setRelevantOriginalChunkIds(jsonIds(entity.getRelevantOriginalChunkIds()));
        detail.setIrrelevantOriginalChunkIds(jsonIds(entity.getIrrelevantOriginalChunkIds()));
        detail.setMissedRelevantChunkIds(jsonIds(entity.getMissedRelevantChunkIds()));
        detail.setTruePositive(entity.getTruePositive());
        detail.setFalseNegative(entity.getFalseNegative());
        detail.setFalsePositive(entity.getFalsePositive());
        detail.setRecallScore(entity.getRecallScore());
        detail.setPrecisionScore(entity.getPrecisionScore());
        detail.setEvaluatedCandidateCount(entity.getEvaluatedCandidateCount());
        detail.setEvaluatedTopN(entity.getEvaluatedTopN());
        detail.setModelName(entity.getModelName());
        detail.setPromptVersion(entity.getPromptVersion());
        detail.setExplanation(entity.getExplanation());
        detail.setStatus(entity.getStatus());
        detail.setErrorMessage(entity.getErrorMessage());
        detail.setEvaluatedAt(entity.getEvaluatedAt());
        return detail;
    }

    private List<Long> parseChunkIds(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        for (String part : value.split(",")) {
            try {
                ids.add(Long.parseLong(part.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return ids.stream().distinct().toList();
    }

    private List<Long> jsonIds(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return JsonUtils.fromJsonList(json, Long.class);
        } catch (Exception ex) {
            return List.of();
        }
    }

    private BigDecimal weightedAverage(List<RagRetrievalMetricDailySummaryEntity> summaries, boolean recall) {
        BigDecimal weightedSum = BigDecimal.ZERO;
        long weight = 0L;
        for (RagRetrievalMetricDailySummaryEntity summary : summaries) {
            long successCount = nullToZero(summary.getSuccessCount());
            if (successCount <= 0) {
                continue;
            }
            BigDecimal score = recall ? nullToZero(summary.getAverageRecall()) : nullToZero(summary.getAveragePrecision());
            weightedSum = weightedSum.add(score.multiply(BigDecimal.valueOf(successCount)));
            weight += successCount;
        }
        if (weight == 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return weightedSum.divide(BigDecimal.valueOf(weight), 4, RoundingMode.HALF_UP);
    }

    private long sumLong(List<RagRetrievalMetricDailySummaryEntity> summaries, java.util.function.Function<RagRetrievalMetricDailySummaryEntity, Long> getter) {
        return summaries.stream().map(getter).filter(Objects::nonNull).mapToLong(Long::longValue).sum();
    }

    private BigDecimal average(List<BigDecimal> values) {
        List<BigDecimal> valid = values.stream().filter(Objects::nonNull).toList();
        if (valid.isEmpty()) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        BigDecimal sum = valid.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(valid.size()), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal toScore(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP) : value;
    }

    private long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
