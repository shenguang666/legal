package com.legal.court.dto;

import com.legal.enums.CourtArgumentSpeaker;
import lombok.Data;

/**
 * 智能小法庭流式庭审事件。
 */
@Data
public class CourtHearingStreamEvent {

    /** 事件类型。 */
    private String type;

    /** 案件ID。 */
    private Long caseId;

    /** 庭审轮次ID。 */
    private Long roundId;

    /** 庭审尝试编号。 */
    private Integer attemptId;

    /** 当前发言角色。 */
    private CourtArgumentSpeaker speaker;

    /** 事件文本内容。 */
    private String content;

    /** 模型名称。 */
    private String modelName;

    /** token 消耗。 */
    private Integer tokenUsage;

    public static CourtHearingStreamEvent of(String type, Long caseId, Long roundId, Integer attemptId, CourtArgumentSpeaker speaker, String content) {
        CourtHearingStreamEvent event = new CourtHearingStreamEvent();
        event.setType(type);
        event.setCaseId(caseId);
        event.setRoundId(roundId);
        event.setAttemptId(attemptId);
        event.setSpeaker(speaker);
        event.setContent(content);
        return event;
    }
}
