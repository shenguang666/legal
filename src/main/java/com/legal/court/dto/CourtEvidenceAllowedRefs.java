package com.legal.court.dto;

import lombok.Data;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 智能小法庭庭审轮次允许引用的证据白名单。
 */
@Data
public class CourtEvidenceAllowedRefs {

    /** 租户ID。 */
    private Long tenantId;
    /** 案件ID。 */
    private Long caseId;
    /** 庭审轮次ID。 */
    private Long roundId;
    /** 允许引用的证据登记ID集合。 */
    private Set<Long> evidenceIds = new HashSet<>();
    /** 允许引用的父分块ID集合。 */
    private Set<Long> parentChunkIds = new HashSet<>();
    /** 允许引用的子分块或普通分块ID集合。 */
    private Set<Long> childChunkIds = new HashSet<>();
    /** 允许引用的证据与分块映射列表。 */
    private List<RefItem> refs;

    /**
     * 判断证据登记ID是否允许引用。
     */
    public boolean containsEvidenceId(Long evidenceId) {
        return evidenceId != null && evidenceIds.contains(evidenceId);
    }

    /**
     * 判断切片ID是否允许引用。
     */
    public boolean containsChunkId(Long chunkId) {
        return chunkId != null && (parentChunkIds.contains(chunkId) || childChunkIds.contains(chunkId));
    }

    /**
     * 单条允许引用的证据与分块映射。
     */
    @Data
    public static class RefItem {

        /** 证据登记ID。 */
        private Long evidenceId;
        /** 证据关联文档ID。 */
        private Long documentId;
        /** 父分块ID，普通分块场景下等于 childChunkId。 */
        private Long parentChunkId;
        /** 原始命中的子分块ID或普通分块ID。 */
        private Long childChunkId;
        /** 切片类型。 */
        private String chunkType;
    }
}
