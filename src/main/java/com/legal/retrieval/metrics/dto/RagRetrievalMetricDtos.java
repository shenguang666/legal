package com.legal.retrieval.metrics.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class RagRetrievalMetricDtos {

    private RagRetrievalMetricDtos() {
    }

    @Data
    public static class Summary {
        private BigDecimal averageRecall;
        private BigDecimal averagePrecision;
        private long evaluatedCount;
        private long validMessageCount;
        private long filteredCount;
        private long successCount;
        private long failedCount;
        private long skippedCount;
        private int candidateTopN;
        private String evaluationVersion;
        private List<TrendPoint> trends;
        private List<DailySummary> dailySummaries;
    }

    @Data
    public static class TrendPoint {
        private LocalDate date;
        private BigDecimal averageRecall;
        private BigDecimal averagePrecision;
        private long totalRagMessageCount;
        private long validMessageCount;
        private long successCount;
        private long failedCount;
        private long skippedCount;
        private long filteredCount;
        private String status;
        private Long dailySummaryId;
    }

    @Data
    public static class DailySummary {
        private Long id;
        private LocalDate metricDate;
        private String evaluationVersion;
        private long totalRagMessageCount;
        private long validMessageCount;
        private long filteredMessageCount;
        private long successCount;
        private long failedCount;
        private long skippedCount;
        private BigDecimal averageRecall;
        private BigDecimal averagePrecision;
        private Integer candidateTopN;
        private String status;
        private String errorMessage;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
    }

    @Data
    public static class DailySummaryPage {
        private long total;
        private int pageNo;
        private int pageSize;
        private List<DailySummary> items;
    }

    @Data
    public static class DetailPage {
        private long total;
        private int pageNo;
        private int pageSize;
        private List<Detail> items;
    }

    @Data
    public static class Detail {
        private Long id;
        private Long dailySummaryId;
        private Long retrievalLogId;
        private String queryText;
        private List<Long> originalHitChunkIds;
        private List<Long> relevantOriginalChunkIds;
        private List<Long> irrelevantOriginalChunkIds;
        private List<Long> missedRelevantChunkIds;
        private Integer truePositive;
        private Integer falseNegative;
        private Integer falsePositive;
        private BigDecimal recallScore;
        private BigDecimal precisionScore;
        private Integer evaluatedCandidateCount;
        private Integer evaluatedTopN;
        private String modelName;
        private String promptVersion;
        private String explanation;
        private String status;
        private String errorMessage;
        private LocalDateTime evaluatedAt;
    }

    @Data
    public static class RunResult {
        private int selected;
        private int success;
        private int failed;
        private int skipped;
    }
}
