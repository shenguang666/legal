package com.legal.court.dto;

import com.legal.enums.CourtArgumentStance;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 智能小法庭 AI 角色结构化输出 DTO。
 */
@Data
public class CourtAgentResponse {

    /** 发言正文。 */
    private String content;
    /** 观点立场。 */
    private CourtArgumentStance stance;
    /** 观点理由或推理过程。 */
    private String rationale;
    /** 证据引用列表。 */
    private List<CourtAgentEvidenceRef> evidenceRefs = new ArrayList<>();
}
