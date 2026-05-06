package com.legal.court.health;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * 智能小法庭 Neo4j 健康检查。
 */
@Component("neo4jSmartCourt")
public class CourtNeo4jHealthIndicator implements HealthIndicator {

    private final Driver driver;

    public CourtNeo4jHealthIndicator(Driver driver) {
        this.driver = driver;
    }

    @Override
    public Health health() {
        try (Session session = driver.session()) {
            String result = session.run("RETURN 'ok' AS status").single().get("status").asString();
            return Health.up()
                    .withDetail("component", "smart-court-neo4j")
                    .withDetail("status", result)
                    .build();
        } catch (Exception ex) {
            return Health.status("DEGRADED")
                    .withDetail("component", "smart-court-neo4j")
                    .withDetail("degraded", true)
                    .withDetail("message", ex.getMessage())
                    .build();
        }
    }
}
