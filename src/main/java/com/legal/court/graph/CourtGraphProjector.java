package com.legal.court.graph;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.legal.config.SmartCourtProperties;
import com.legal.court.entity.CourtGraphEventEntity;
import com.legal.court.graph.repository.CaseGraphRepository;
import com.legal.court.mapper.CourtGraphEventMapper;
import com.legal.court.observability.CourtMetricsService;
import com.legal.court.service.CourtSuggestionService;
import com.legal.enums.CourtGraphEventType;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 智能小法庭 Neo4j 图谱事件投影器。
 */
@Slf4j
@Component
public class CourtGraphProjector {

    private static final Set<String> ALLOWED_LABELS = Set.of(
            "Case", "Party", "Claim", "Defense", "Fact", "Evidence", "Document", "Chunk", "Clause",
            "Obligation", "Breach", "Amount", "Date", "Argument", "Risk", "Gap", "LegalBasis", "JudgmentPoint"
    );
    private static final Set<String> ALLOWED_RELATIONS = Set.of(
            "HAS_PARTY", "RAISES_CLAIM", "RAISES_DEFENSE", "ASSERTS_FACT", "SUPPORTED_BY", "CONTRADICTED_BY",
            "DERIVED_FROM", "QUOTES_CLAUSE", "PROVES_AMOUNT", "PROVES_DATE", "CREATES_OBLIGATION",
            "BREACHES_OBLIGATION", "SUPPORTS_ARGUMENT", "CHALLENGES_ARGUMENT", "HAS_RISK", "HAS_GAP",
            "NEEDS_EVIDENCE", "SUPPORTS_JUDGMENT", "HAS_EVIDENCE", "PRESENTS_ARGUMENT",
            "ADVISES_ARGUMENT", "OPPOSES_ARGUMENT", "REBUTS_ARGUMENT", "SUMMARIZES_ARGUMENT"
    );

    private final CourtGraphEventMapper courtGraphEventMapper;
    private final CaseGraphRepository caseGraphRepository;
    private final Driver driver;
    private final SmartCourtProperties properties;
    private final ObjectMapper objectMapper;
    private final CourtSuggestionService courtSuggestionService;
    private final CourtMetricsService courtMetricsService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public CourtGraphProjector(CourtGraphEventMapper courtGraphEventMapper,
                               CaseGraphRepository caseGraphRepository,
                               Driver driver,
                               SmartCourtProperties properties,
                               ObjectMapper objectMapper,
                               CourtSuggestionService courtSuggestionService,
                               CourtMetricsService courtMetricsService) {
        this.courtGraphEventMapper = courtGraphEventMapper;
        this.caseGraphRepository = caseGraphRepository;
        this.driver = driver;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.courtSuggestionService = courtSuggestionService;
        this.courtMetricsService = courtMetricsService;
    }

    /**
     * 定时投影 PENDING 图谱事件。
     */
    @Scheduled(fixedDelayString = "${legal.smart-court.graph.projector-poll-interval-ms:2000}")
    public void poll() {
        if (!properties.isEnabled() || !properties.getGraph().isProjectorEnabled()) {
            return;
        }
        if (!running.compareAndSet(false, true)) {
            return;
        }
        try {
            List<CourtGraphEventEntity> events = courtGraphEventMapper.selectReadyPending(Math.max(1, properties.getGraph().getProjectorBatchSize()));
            courtMetricsService.setGraphEventPending(events.size());
            for (CourtGraphEventEntity event : events) {
                applyOne(event);
            }
        } finally {
            running.set(false);
        }
    }

    /**
     * 重建指定案件图谱，先删除子图再按事件流重新投影。
     */
    public void rebuildCase(Long tenantId, Long caseId) {
        Neo4jTenantContext.withTenant(tenantId, () -> caseGraphRepository.detachDeleteByTenantIdAndCaseId(tenantId, caseId));
        Long afterEventId = 0L;
        int pageSize = Math.max(1, properties.getGraph().getRebuildPageSize());
        while (true) {
            List<CourtGraphEventEntity> events = courtGraphEventMapper.selectCaseEventsForReplay(tenantId, caseId, afterEventId, pageSize);
            if (events.isEmpty()) {
                return;
            }
            for (CourtGraphEventEntity event : events) {
                applyPayload(event, readPayload(event));
                courtGraphEventMapper.markAppliedAfterReplay(event.getEventId());
                afterEventId = event.getEventId();
            }
        }
    }

    void applyOne(CourtGraphEventEntity event) {
        try {
            CourtGraphEventPayload payload = readPayload(event);
            validateTenant(event, payload);
            if (event.getEventType() == CourtGraphEventType.DELETE_CASE_GRAPH) {
                Neo4jTenantContext.withTenant(event.getTenantId(), () -> caseGraphRepository.detachDeleteByTenantIdAndCaseId(event.getTenantId(), event.getCaseId()));
            } else {
                applyPayload(event, payload);
            }
            courtGraphEventMapper.markApplied(event.getEventId());
            log.info("graph.event.apply tenantId={} caseId={} roundId={} eventId={} eventType={} status=APPLIED",
                    event.getTenantId(), event.getCaseId(), event.getRoundId(), event.getEventId(), event.getEventType());
            refreshSuggestionsAfterProjection(event);
        } catch (Exception ex) {
            handleFailure(event, ex);
        }
    }

    private void refreshSuggestionsAfterProjection(CourtGraphEventEntity event) {
        try {
            courtSuggestionService.refreshSuggestions(event.getTenantId(), event.getCaseId(), event.getRoundId());
        } catch (Exception ex) {
            log.warn("智能小法庭投影后刷新补证建议失败 eventId={} caseId={} error={}", event.getEventId(), event.getCaseId(), ex.getMessage());
        }
    }

    private CourtGraphEventPayload readPayload(CourtGraphEventEntity event) {
        try {
            return objectMapper.readValue(event.getPayloadJson(), CourtGraphEventPayload.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException("图谱事件载荷解析失败 eventId=" + event.getEventId(), ex);
        }
    }

    private void applyPayload(CourtGraphEventEntity event, CourtGraphEventPayload payload) {
        if (event.getEventType() == CourtGraphEventType.UPSERT_NODE
                || event.getEventType() == CourtGraphEventType.UPDATE_NODE_STATUS
                || event.getEventType() == CourtGraphEventType.INVALIDATE_NODE) {
            assertNodeLimit(event, payload);
            upsertNode(event, payload);
            return;
        }
        if (event.getEventType() == CourtGraphEventType.UPSERT_RELATION) {
            upsertRelation(event, payload);
            return;
        }
        throw new IllegalArgumentException("不支持的图谱事件类型: " + event.getEventType());
    }

    private void upsertNode(CourtGraphEventEntity event, CourtGraphEventPayload payload) {
        assertAllowedLabel(payload.getLabel());
        Map<String, Object> props = baseProps(event, payload);
        String cypher = "MERGE (n:" + payload.getLabel() + " {tenantId:$tenantId, businessId:$businessId}) SET n += $props";
        try (Session session = driver.session()) {
            session.run(cypher, Map.of("tenantId", event.getTenantId(), "businessId", payload.getBusinessId(), "props", props)).consume();
        }
    }

    private void upsertRelation(CourtGraphEventEntity event, CourtGraphEventPayload payload) {
        assertAllowedLabel(payload.getFromLabel());
        assertAllowedLabel(payload.getToLabel());
        assertAllowedRelation(payload.getRelationType());
        Map<String, Object> props = baseProps(event, payload);
        props.put("sourceBusinessId", payload.getFromBusinessId());
        props.put("targetBusinessId", payload.getToBusinessId());
        String cypher = "MATCH (a:" + payload.getFromLabel() + " {tenantId:$tenantId, businessId:$fromBusinessId}) "
                + "MATCH (b:" + payload.getToLabel() + " {tenantId:$tenantId, businessId:$toBusinessId}) "
                + "MERGE (a)-[r:" + payload.getRelationType() + " {tenantId:$tenantId, businessId:$businessId}]->(b) SET r += $props";
        try (Session session = driver.session()) {
            session.run(cypher, Map.of(
                    "tenantId", event.getTenantId(),
                    "fromBusinessId", payload.getFromBusinessId(),
                    "toBusinessId", payload.getToBusinessId(),
                    "businessId", payload.getBusinessId(),
                    "props", props
            )).consume();
        }
    }

    private Map<String, Object> baseProps(CourtGraphEventEntity event, CourtGraphEventPayload payload) {
        Map<String, Object> props = new HashMap<>(payload.getProperties() == null ? Map.of() : payload.getProperties());
        props.put("tenantId", event.getTenantId());
        props.put("caseId", event.getCaseId());
        props.put("eventId", event.getEventId());
        props.put("businessId", payload.getBusinessId());
        props.putIfAbsent("schemaVersion", CourtGraphSchema.SCHEMA_VERSION);
        return props;
    }

    private void assertNodeLimit(CourtGraphEventEntity event, CourtGraphEventPayload payload) {
        if (payload.getLabel() == null || payload.getBusinessId() == null) {
            return;
        }
        long nodeCount = Neo4jTenantContext.withTenant(event.getTenantId(), () -> caseGraphRepository.countNodesByTenantIdAndCaseId(event.getTenantId(), event.getCaseId()));
        if (nodeCount >= properties.getGraph().getMaxNodesPerCase()) {
            throw new IllegalStateException("案件图谱节点数超过上限 caseId=" + event.getCaseId() + ", nodeCount=" + nodeCount);
        }
    }

    private void validateTenant(CourtGraphEventEntity event, CourtGraphEventPayload payload) {
        Object payloadTenantId = payload.getProperties() == null ? null : payload.getProperties().get("tenantId");
        if (payloadTenantId != null && !String.valueOf(event.getTenantId()).equals(String.valueOf(payloadTenantId))) {
            throw new IllegalArgumentException("图谱事件 payload tenantId 与事件租户不一致");
        }
    }

    private void handleFailure(CourtGraphEventEntity event, Exception ex) {
        int retryCount = (event.getRetryCount() == null ? 0 : event.getRetryCount()) + 1;
        String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        if (retryCount >= Math.max(1, properties.getGraph().getProjectorMaxRetries())) {
            courtGraphEventMapper.markDead(event.getEventId(), retryCount, truncate(message));
            courtMetricsService.incrementGraphEventDead();
            log.warn("智能小法庭图谱事件进入死信 eventId={} caseId={} error={}", event.getEventId(), event.getCaseId(), message);
            return;
        }
        courtGraphEventMapper.markRetry(event.getEventId(), retryCount, truncate(message), backoffSeconds(retryCount));
    }

    private long backoffSeconds(int retryCount) {
        return Math.min(300, 1L << Math.min(8, retryCount));
    }

    private void assertAllowedLabel(String label) {
        if (!StringUtils.hasText(label) || !ALLOWED_LABELS.contains(label)) {
            throw new IllegalArgumentException("非法 Neo4j 节点标签: " + label);
        }
    }

    private void assertAllowedRelation(String relationType) {
        if (!StringUtils.hasText(relationType) || !ALLOWED_RELATIONS.contains(relationType)) {
            throw new IllegalArgumentException("非法 Neo4j 关系类型: " + relationType);
        }
    }

    private String truncate(String message) {
        return message == null || message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
