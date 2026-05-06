package com.legal.court.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 智能小法庭指标服务。
 */
@Service
public class CourtMetricsService {

    private final Counter graphEventDeadCounter;
    private final Counter graphQueryDbHitsCounter;
    private final Counter suggestionHighCounter;
    private final AtomicLong graphEventPendingGauge = new AtomicLong(0);

    public CourtMetricsService(MeterRegistry meterRegistry) {
        this.graphEventDeadCounter = Counter.builder("court.graph.event.dead")
                .description("智能小法庭图谱事件死信数量")
                .register(meterRegistry);
        this.graphQueryDbHitsCounter = Counter.builder("court.graph.query.dbHits")
                .description("智能小法庭图谱查询数据库命中近似数量")
                .register(meterRegistry);
        this.suggestionHighCounter = Counter.builder("court.suggestion.high_count")
                .description("智能小法庭高严重度补证建议数量")
                .register(meterRegistry);
        Gauge.builder("court.graph.event.pending", graphEventPendingGauge, AtomicLong::get)
                .description("智能小法庭图谱待投影事件数量")
                .register(meterRegistry);
    }

    /**
     * 记录图谱事件进入死信。
     */
    public void incrementGraphEventDead() {
        graphEventDeadCounter.increment();
    }

    /**
     * 更新待投影事件数量。
     */
    public void setGraphEventPending(long pending) {
        graphEventPendingGauge.set(Math.max(0, pending));
    }

    /**
     * 记录图谱查询近似 dbHits。
     */
    public void recordGraphQueryDbHits(long dbHits) {
        if (dbHits > 0) {
            graphQueryDbHitsCounter.increment(dbHits);
        }
    }

    /**
     * 记录高严重度补证建议。
     */
    public void recordHighSuggestions(long count) {
        if (count > 0) {
            suggestionHighCounter.increment(count);
        }
    }
}
