package com.legal.token.metrics.service;

import com.legal.config.TokenUsageMetricProperties;
import com.legal.token.metrics.dto.TokenUsageMetricDtos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class TokenUsageMetricWorker {

    private static final Logger log = LoggerFactory.getLogger(TokenUsageMetricWorker.class);

    private final TokenUsageMetricProperties properties;
    private final TokenUsageMetricService metricService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public TokenUsageMetricWorker(TokenUsageMetricProperties properties,
                                  TokenUsageMetricService metricService) {
        this.properties = properties;
        this.metricService = metricService;
    }

    @Scheduled(cron = "${token.metric.usage.worker.cron:0 0/5 2-5 * * ?}")
    public void runDailyEvaluation() {
        if (!properties.isEnabled()) {
            return;
        }
        if (!running.compareAndSet(false, true)) {
            log.warn("Token usage metric worker skipped because previous run is still running");
            return;
        }
        try {
            TokenUsageMetricDtos.RunResult result = metricService.evaluateScheduledRun();
            log.info("Token usage metric worker date={} selected={} success={} failed={} skipped={} message={}",
                    result.getMetricDate(), result.getSelected(), result.getSuccess(),
                    result.getFailed(), result.getSkipped(), result.getMessage());
        } finally {
            running.set(false);
        }
    }
}
