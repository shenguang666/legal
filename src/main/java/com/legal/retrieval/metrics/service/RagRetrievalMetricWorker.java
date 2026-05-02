package com.legal.retrieval.metrics.service;

import com.legal.config.RagRetrievalMetricProperties;
import com.legal.retrieval.metrics.dto.RagRetrievalMetricDtos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RagRetrievalMetricWorker {

    private static final Logger log = LoggerFactory.getLogger(RagRetrievalMetricWorker.class);

    private final RagRetrievalMetricProperties properties;
    private final RagRetrievalMetricService metricService;

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
        RagRetrievalMetricDtos.RunResult result = metricService.evaluateDate(null);
        log.info("RAG metric worker selected={} success={} failed={} skipped={}",
                result.getSelected(), result.getSuccess(), result.getFailed(), result.getSkipped());
    }
}
