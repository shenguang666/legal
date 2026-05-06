package com.legal.court.dto;

import lombok.Data;

/**
 * 智能小法庭 AI 输出中的证据引用 DTO。
 */
@Data
public class CourtAgentEvidenceRef {

    /** 证据登记ID。 */
    private Long evidenceId;
    /** 父分块ID。 */
    private Long parentChunkId;
    /** 原始命中的子分块ID或普通分块ID。 */
    private Long childChunkId;
    /** 引用说明。 */
    private String reason;
}
