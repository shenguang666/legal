package com.legal.court.graph;

import com.legal.config.SmartCourtProperties;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 智能小法庭 Neo4j schema 初始化器。
 */
@Slf4j
@Component
public class Neo4jSchemaInitializer implements ApplicationRunner {

    private static final List<String> UNIQUE_CONSTRAINTS = List.of(
            "CREATE CONSTRAINT court_case_business_id IF NOT EXISTS FOR (n:Case) REQUIRE (n.tenantId, n.businessId) IS UNIQUE",
            "CREATE CONSTRAINT court_party_business_id IF NOT EXISTS FOR (n:Party) REQUIRE (n.tenantId, n.businessId) IS UNIQUE",
            "CREATE CONSTRAINT court_claim_business_id IF NOT EXISTS FOR (n:Claim) REQUIRE (n.tenantId, n.businessId) IS UNIQUE",
            "CREATE CONSTRAINT court_defense_business_id IF NOT EXISTS FOR (n:Defense) REQUIRE (n.tenantId, n.businessId) IS UNIQUE",
            "CREATE CONSTRAINT court_fact_business_id IF NOT EXISTS FOR (n:Fact) REQUIRE (n.tenantId, n.businessId) IS UNIQUE",
            "CREATE CONSTRAINT court_evidence_business_id IF NOT EXISTS FOR (n:Evidence) REQUIRE (n.tenantId, n.businessId) IS UNIQUE",
            "CREATE CONSTRAINT court_argument_business_id IF NOT EXISTS FOR (n:Argument) REQUIRE (n.tenantId, n.businessId) IS UNIQUE",
            "CREATE CONSTRAINT court_judgment_point_business_id IF NOT EXISTS FOR (n:JudgmentPoint) REQUIRE (n.tenantId, n.businessId) IS UNIQUE"
    );

    private static final List<String> INDEXES = List.of(
            "CREATE INDEX court_evidence_case IF NOT EXISTS FOR (n:Evidence) ON (n.tenantId, n.caseId)",
            "CREATE INDEX court_fact_case_status IF NOT EXISTS FOR (n:Fact) ON (n.tenantId, n.caseId, n.status)",
            "CREATE INDEX court_claim_case IF NOT EXISTS FOR (n:Claim) ON (n.tenantId, n.caseId)",
            "CREATE INDEX court_gap_case IF NOT EXISTS FOR (n:Gap) ON (n.tenantId, n.caseId)",
            "CREATE INDEX court_risk_case IF NOT EXISTS FOR (n:Risk) ON (n.tenantId, n.caseId)"
    );

    private final Driver driver;
    private final SmartCourtProperties properties;

    public Neo4jSchemaInitializer(Driver driver, SmartCourtProperties properties) {
        this.driver = driver;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }
        try (Session session = driver.session()) {
            for (String cypher : UNIQUE_CONSTRAINTS) {
                session.run(cypher).consume();
            }
            for (String cypher : INDEXES) {
                session.run(cypher).consume();
            }
            log.info("智能小法庭 Neo4j schema 初始化完成，schemaVersion={}", CourtGraphSchema.SCHEMA_VERSION);
        } catch (RuntimeException ex) {
            log.warn("智能小法庭 Neo4j schema 初始化失败，图谱功能将暂时降级: {}", ex.getMessage());
        }
    }
}
