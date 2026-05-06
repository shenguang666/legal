package com.legal.court.dto;

import com.legal.enums.CourtArgumentSpeaker;
import com.legal.enums.CourtArgumentStance;
import com.legal.enums.CourtHearingStage;
import com.legal.enums.CourtHearingState;
import com.legal.enums.CourtPartyRole;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 智能小法庭庭审历史记录 DTO。
 */
@Data
public class CourtHearingRecordDto {

    /** 庭审轮次ID。 */
    private Long roundId;
    /** 案件内庭审轮次顺序号。 */
    private Integer roundNo;
    /** 庭审阶段。 */
    private CourtHearingStage stage;
    /** 庭审轮次状态。 */
    private CourtHearingState state;
    /** 庭审开始时间。 */
    private LocalDateTime startedAt;
    /** 庭审结束时间。 */
    private LocalDateTime endedAt;
    /** 庭审轮次消息列表。 */
    private List<MessageDto> messages;

    /** 智能小法庭庭审消息 DTO。 */
    @Data
    public static class MessageDto {

        /** 庭审消息ID。 */
        private Long messageId;
        /** 庭审观点ID。 */
        private Long argumentId;
        /** 发言角色。 */
        private CourtArgumentSpeaker speaker;
        /** 发言所代表的当事人角色。 */
        private CourtPartyRole speakerParty;
        /** 观点立场。 */
        private CourtArgumentStance stance;
        /** 展示文本内容。 */
        private String content;
        /** 观点理由。 */
        private String rationale;
        /** 证据引用 JSON。 */
        private String evidenceRefsJson;
        /** token 消耗。 */
        private Integer tokenUsage;
        /** 创建时间。 */
        private LocalDateTime createdAt;
    }
}
