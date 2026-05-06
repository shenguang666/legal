package com.legal.config;

import org.neo4j.cypherdsl.core.renderer.Configuration;
import org.neo4j.cypherdsl.core.renderer.Dialect;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 智能小法庭 Neo4j 配置。
 * Spring Boot 已根据 spring.neo4j.* 自动装配 Driver / Neo4jClient / Neo4jTemplate / Neo4jTransactionManager，
 * 这里仅显式启用 Repository 扫描、事务管理，并锁定 Cypher DSL 方言为 Neo4j 5。
 */
@org.springframework.context.annotation.Configuration
@EnableConfigurationProperties(SmartCourtProperties.class)
@EnableNeo4jRepositories(basePackages = "com.legal.court.graph.repository")
@EnableTransactionManagement
public class Neo4jConfig {

    /**
     * 显式声明 Cypher DSL 使用 Neo4j 5 方言，保证生成的 Cypher 与部署的 Neo4j 5 兼容。
     */
    @Bean
    public Configuration cypherDslConfiguration() {
        return Configuration.newConfig().withDialect(Dialect.NEO4J_5).build();
    }
}
