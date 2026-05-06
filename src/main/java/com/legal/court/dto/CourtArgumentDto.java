package com.legal.court.dto;

import com.legal.enums.CourtArgumentSpeaker;
import com.legal.enums.CourtArgumentStance;
import com.legal.enums.CourtPartyRole;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 智能小法庭庭审观点 DTO。
 */
@Data
public class CourtArgumentDto {

    /** 庭审观点主键ID。 */
    private Long argumentId;
    /** 租户ID。 */
    private Long tenantId;
    /** 案件ID。 */
    private Long caseId;
    /** 所属庭审轮次ID。 */
    private Long roundId;
    /** 关联的庭审消息ID。 */
    private Long messageId;
    /** 发言角色。 */
    private CourtArgumentSpeaker speakerRole;
    /** 发言所代表的当事人角色。 */
    private CourtPartyRole speakerParty;
    /** 观点立场。 */
    private CourtArgumentStance stance;
    /** 观点正文。 */
    private String content;
    /** 观点理由或推理过程。 */
    private String rationale;
    /** 关联的证据引用 JSON。 */
    private String evidenceRefsJson;
    /** 被本观点反驳的其他观点 ID JSON 数组。 */
    private String challengedArgumentIdsJson;
    /** 通过三层校验的证据引用条数。 */
    private Integer evidenceVerifiedCount;
    /** 被三层校验丢弃的证据引用条数。 */
    private Integer evidenceDroppedCount;
    /** 创建时间。 */
    private LocalDateTime createdAt;
}
