package com.legal.court.graph;

import com.legal.config.SmartCourtProperties;
import com.legal.court.dto.CourtGraphSnapshotDto;
import com.legal.court.entity.CourtCaseEvidenceEntity;
import com.legal.court.mapper.CourtCaseEvidenceMapper;
import com.legal.court.mapper.CourtGraphEventMapper;
import com.legal.court.observability.CourtMetricsService;
import com.legal.enums.CourtGraphState;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能小法庭图谱查询服务。
 */
@Slf4j
@Service
public class CourtGraphQueryService {

    private static final String CASE_SUBGRAPH_CYPHER = """
            MATCH (n {tenantId:$tenantId, caseId:$caseId})
            OPTIONAL MATCH (n)-[r]-(m {tenantId:$tenantId, caseId:$caseId})
            RETURN n, r, m
            LIMIT $limit
            """;

    private final Driver driver;
    private final SmartCourtProperties properties;
    private final CourtGraphEventMapper courtGraphEventMapper;
    private final CourtCaseEvidenceMapper courtCaseEvidenceMapper;
    private final CourtMetricsService courtMetricsService;

    public CourtGraphQueryService(Driver driver,
                                  SmartCourtProperties properties,
                                  CourtGraphEventMapper courtGraphEventMapper,
                                  CourtCaseEvidenceMapper courtCaseEvidenceMapper,
                                  CourtMetricsService courtMetricsService) {
        this.driver = driver;
        this.properties = properties;
        this.courtGraphEventMapper = courtGraphEventMapper;
        this.courtCaseEvidenceMapper = courtCaseEvidenceMapper;
        this.courtMetricsService = courtMetricsService;
    }

    /**
     * 查询案件默认证据链图谱快照。
     */
    public CourtGraphSnapshotDto queryCaseGraph(Long tenantId, Long caseId, String focusClaimId, Integer hops, Integer limit) {
        int safeHops = Math.min(Math.max(1, hops == null ? properties.getGraph().getDefaultFocusHops() : hops), properties.getGraph().getMaxHops());
        int safeLimit = Math.max(1, Math.min(limit == null ? properties.getGraph().getDefaultFocusNodeLimit() : limit, properties.getGraph().getMaxNodesPerCase()));
        long pending = courtGraphEventMapper.countPendingByCase(tenantId, caseId);
        try {
            CourtGraphSnapshotDto snapshot = queryNeo4j(tenantId, caseId, safeLimit);
            snapshot.setGraphState(pending > 0 ? CourtGraphState.PROJECTING.getCode() : CourtGraphState.READY.getCode());
            snapshot.setPendingEventCount(pending);
            courtMetricsService.recordGraphQueryDbHits(snapshot.getNodeCount() + snapshot.getEdgeCount());
            snapshot.setTruncated(snapshot.getNodeCount() >= safeLimit);
            if (safeHops < (hops == null ? safeHops : hops)) {
                snapshot.setWarning("请求跳数超过系统上限，已按配置裁剪");
            }
            return snapshot;
        } catch (RuntimeException ex) {
            log.warn("智能小法庭 Neo4j 图谱查询失败，降级返回 MySQL 证据列表 tenantId={} caseId={} error={}", tenantId, caseId, ex.getMessage());
            return fallbackFromMysql(tenantId, caseId, pending);
        }
    }

    private CourtGraphSnapshotDto queryNeo4j(Long tenantId, Long caseId, int limit) {
        Map<String, CourtGraphSnapshotDto.NodeDto> nodes = new LinkedHashMap<>();
        Map<String, CourtGraphSnapshotDto.EdgeDto> edges = new LinkedHashMap<>();
        try (Session session = driver.session()) {
            List<Record> records = session.run(CASE_SUBGRAPH_CYPHER, Map.of("tenantId", tenantId, "caseId", caseId, "limit", limit)).list();
            for (Record record : records) {
                addNode(nodes, record.get("n"));
                addNode(nodes, record.get("m"));
                addEdge(edges, record.get("r"));
            }
        }
        return snapshot(tenantId, caseId, new ArrayList<>(nodes.values()), new ArrayList<>(edges.values()));
    }

    private CourtGraphSnapshotDto fallbackFromMysql(Long tenantId, Long caseId, long pending) {
        List<CourtGraphSnapshotDto.NodeDto> nodes = new ArrayList<>();
        List<CourtCaseEvidenceEntity> evidences = courtCaseEvidenceMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CourtCaseEvidenceEntity>()
                .eq(CourtCaseEvidenceEntity::getTenantId, tenantId)
                .eq(CourtCaseEvidenceEntity::getCaseId, caseId));
        for (CourtCaseEvidenceEntity evidence : evidences) {
            CourtGraphSnapshotDto.NodeDto node = new CourtGraphSnapshotDto.NodeDto();
            node.setBusinessId("evidence-" + evidence.getEvidenceId());
            node.setType("Evidence");
            node.setLabel(evidence.getDisplayName());
            node.setStatus(evidence.getStatus() == null ? null : evidence.getStatus().getCode());
            node.setProperties(Map.of("evidenceId", evidence.getEvidenceId(), "documentId", evidence.getDocumentId()));
            nodes.add(node);
        }
        CourtGraphSnapshotDto snapshot = snapshot(tenantId, caseId, nodes, List.of());
        snapshot.setGraphState(CourtGraphState.UNAVAILABLE.getCode());
        snapshot.setPendingEventCount(pending);
        snapshot.setWarning("Neo4j 暂不可用，已降级为 MySQL 证据列表");
        return snapshot;
    }

    private CourtGraphSnapshotDto snapshot(Long tenantId, Long caseId, List<CourtGraphSnapshotDto.NodeDto> nodes, List<CourtGraphSnapshotDto.EdgeDto> edges) {
        CourtGraphSnapshotDto snapshot = new CourtGraphSnapshotDto();
        snapshot.setTenantId(tenantId);
        snapshot.setCaseId(caseId);
        snapshot.setSchemaVersion(CourtGraphSchema.SCHEMA_VERSION);
        snapshot.setNodes(nodes);
        snapshot.setEdges(edges);
        snapshot.setNodeCount(nodes.size());
        snapshot.setEdgeCount(edges.size());
        snapshot.setTruncated(false);
        return snapshot;
    }

    private void addNode(Map<String, CourtGraphSnapshotDto.NodeDto> nodes, Value value) {
        if (value == null || value.isNull()) {
            return;
        }
        org.neo4j.driver.types.Node node = value.asNode();
        Map<String, Object> props = node.asMap();
        String businessId = String.valueOf(props.getOrDefault("businessId", node.id()));
        nodes.computeIfAbsent(businessId, ignored -> {
            CourtGraphSnapshotDto.NodeDto dto = new CourtGraphSnapshotDto.NodeDto();
            dto.setBusinessId(businessId);
            dto.setType(node.labels().iterator().hasNext() ? node.labels().iterator().next() : "Node");
            dto.setLabel(String.valueOf(props.getOrDefault("title", props.getOrDefault("displayName", businessId))));
            dto.setStatus(props.get("status") == null ? null : String.valueOf(props.get("status")));
            dto.setProperties(props);
            return dto;
        });
    }

    private void addEdge(Map<String, CourtGraphSnapshotDto.EdgeDto> edges, Value value) {
        if (value == null || value.isNull()) {
            return;
        }
        org.neo4j.driver.types.Relationship relation = value.asRelationship();
        Map<String, Object> props = relation.asMap();
        String businessId = String.valueOf(props.getOrDefault("businessId", relation.id()));
        edges.computeIfAbsent(businessId, ignored -> {
            CourtGraphSnapshotDto.EdgeDto dto = new CourtGraphSnapshotDto.EdgeDto();
            dto.setBusinessId(businessId);
            dto.setType(relation.type());
            dto.setLabel(relation.type());
            dto.setProperties(props);
            return dto;
        });
    }
}
