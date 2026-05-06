package com.legal.court.graph.repository;

import com.legal.court.graph.node.ClaimNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 智能小法庭诉求图谱仓储。
 */
public interface ClaimGraphRepository extends Neo4jRepository<ClaimNode, String> {

    /**
     * 查询单案件全部诉求节点。
     */
    @Query("MATCH (c:Claim) WHERE c.tenantId = $tenantId AND c.caseId = $caseId RETURN c ORDER BY c.businessId")
    List<ClaimNode> findByTenantIdAndCaseId(@Param("tenantId") Long tenantId, @Param("caseId") Long caseId);

    /**
     * 按租户与业务ID查询诉求节点。
     */
    @Query("MATCH (c:Claim) WHERE c.tenantId = $tenantId AND c.businessId = $businessId RETURN c")
    Optional<ClaimNode> findByTenantIdAndBusinessId(@Param("tenantId") Long tenantId, @Param("businessId") String businessId);

    /**
     * 查询未闭环诉求节点，供补证规则使用。
     */
    @Query("MATCH (c:Claim) WHERE c.tenantId = $tenantId AND c.caseId = $caseId AND NOT EXISTS { MATCH (c)-[:ASSERTS_FACT|SUPPORTED_BY*1..4]->(e:Evidence) WHERE e.tenantId = $tenantId AND e.caseId = $caseId AND coalesce(e.evidenceStatus, e.status) <> 'INVALID' } RETURN c")
    List<ClaimNode> findUnclosedClaims(@Param("tenantId") Long tenantId, @Param("caseId") Long caseId);
}
