package com.legal.retrieval.metrics.service;

import com.legal.config.RagRetrievalMetricProperties;
import com.legal.retrieval.metrics.dto.RagRetrievalMetricDtos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class RagRetrievalMetricWorker {

    private static final Logger log = LoggerFactory.getLogger(RagRetrievalMetricWorker.class);

    private final RagRetrievalMetricProperties properties;
    private final RagRetrievalMetricService metricService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public RagRetrievalMetricWorker(RagRetrievalMetricProperties properties,
                                    RagRetrievalMetricService metricService) {
        this.properties = properties;
        this.metricService = metricService;
    }

    @Scheduled(cron = "${rag.metric.retrieval.worker.cron:0 0/5 2-5 * * ?}")
    public void runDailyEvaluation() {
        if (!properties.isEnabled()) {
            return;
        }
        if (!running.compareAndSet(false, true)) {
            log.warn("RAG metric worker skipped because previous run is still running");
            return;
        }
        try {
            RagRetrievalMetricDtos.RunResult result = metricService.evaluateScheduledRun();
            log.info("RAG metric worker date={} backfill={} selected={} success={} failed={} skipped={} message={}",
                    result.getMetricDate(), result.isBackfill(), result.getSelected(), result.getSuccess(),
                    result.getFailed(), result.getSkipped(), result.getMessage());
        } finally {
            running.set(false);
        }
    }
}
