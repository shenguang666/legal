package com.legal.court.graph.repository;

import com.legal.court.graph.node.EvidenceNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 智能小法庭证据图谱仓储。
 */
public interface EvidenceGraphRepository extends Neo4jRepository<EvidenceNode, String> {

    /**
     * 查询单案件全部证据节点。
     */
    @Query("MATCH (e:Evidence) WHERE e.tenantId = $tenantId AND e.caseId = $caseId RETURN e ORDER BY e.evidenceId")
    List<EvidenceNode> findByTenantIdAndCaseId(@Param("tenantId") Long tenantId, @Param("caseId") Long caseId);

    /**
     * 按租户与业务ID查询证据节点。
     */
    @Query("MATCH (e:Evidence) WHERE e.tenantId = $tenantId AND e.businessId = $businessId RETURN e")
    Optional<EvidenceNode> findByTenantIdAndBusinessId(@Param("tenantId") Long tenantId, @Param("businessId") String businessId);

    /**
     * 按租户、案件与证据登记ID查询证据节点。
     */
    @Query("MATCH (e:Evidence) WHERE e.tenantId = $tenantId AND e.caseId = $caseId AND e.evidenceId = $evidenceId RETURN e")
    Optional<EvidenceNode> findByTenantIdAndCaseIdAndEvidenceId(@Param("tenantId") Long tenantId, @Param("caseId") Long caseId, @Param("evidenceId") Long evidenceId);

    /**
     * 将指定文档在案件图谱中的证据节点标记为失效。
     */
    @Query("MATCH (e:Evidence) WHERE e.tenantId = $tenantId AND e.documentId = $documentId SET e.evidenceStatus = 'INVALID', e.status = 'INVALID' RETURN count(e)")
    long markInvalidByTenantIdAndDocumentId(@Param("tenantId") Long tenantId, @Param("documentId") Long documentId);
}
