package com.legal.court.dto;

import com.legal.enums.CourtArgumentStance;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 智能小法庭 AI 输出证据引用校验结果。
 */
@Data
public class CourtAgentValidationResult {

    /** 校验后的发言正文。 */
    private String content;
    /** 校验后的观点立场。 */
    private CourtArgumentStance stance;
    /** 校验后的观点理由或推理过程。 */
    private String rationale;
    /** 通过校验的证据引用列表。 */
    private List<CourtAgentEvidenceRef> validRefs = new ArrayList<>();
    /** 被丢弃的证据引用列表。 */
    private List<CourtAgentEvidenceRef> droppedRefs = new ArrayList<>();
    /** 通过校验的证据引用数量。 */
    private int evidenceVerifiedCount;
    /** 被丢弃的证据引用数量。 */
    private int evidenceDroppedCount;
    /** 是否因校验失败降级为待证明。 */
    private boolean degradedToPendingProof;
    /** 校验失败原因摘要。 */
    private String failureReason;
}
