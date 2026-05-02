package com.legal.retrieval.metrics.service;

import java.util.List;

public record RagMetricJudgeResult(List<Long> relevantOriginalChunkIds,
                                   List<Long> irrelevantOriginalChunkIds,
                                   List<Long> missedRelevantChunkIds,
                                   String rawSummary,
                                   String explanation,
                                   boolean success,
                                   String failureReason) {
}
