package com.legal.court.graph.repository;

import com.legal.court.graph.node.CaseNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 智能小法庭案件图谱仓储。
 */
public interface CaseGraphRepository extends Neo4jRepository<CaseNode, String> {

    /**
     * 按租户与案件ID查询案件节点。
     */
    @Query("MATCH (c:Case) WHERE c.tenantId = $tenantId AND c.caseId = $caseId RETURN c")
    Optional<CaseNode> findByTenantIdAndCaseId(@Param("tenantId") Long tenantId, @Param("caseId") Long caseId);

    /**
     * 按租户与业务ID查询案件节点。
     */
    @Query("MATCH (c:Case) WHERE c.tenantId = $tenantId AND c.businessId = $businessId RETURN c")
    Optional<CaseNode> findByTenantIdAndBusinessId(@Param("tenantId") Long tenantId, @Param("businessId") String businessId);

    /**
     * 统计单案件图谱节点数量。
     */
    @Query("MATCH (n) WHERE n.tenantId = $tenantId AND n.caseId = $caseId RETURN count(n)")
    long countNodesByTenantIdAndCaseId(@Param("tenantId") Long tenantId, @Param("caseId") Long caseId);

    /**
     * 删除单案件完整子图。
     */
    @Query("MATCH (n) WHERE n.tenantId = $tenantId AND n.caseId = $caseId DETACH DELETE n")
    void detachDeleteByTenantIdAndCaseId(@Param("tenantId") Long tenantId, @Param("caseId") Long caseId);
}
