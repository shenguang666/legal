package com.legal.court.dto;

import lombok.Data;

/**
 * 智能小法庭流式开庭请求。
 */
@Data
public class CourtHearingStreamRequest {

    /** 用户本轮发言内容。 */
    private String userStatement;

    /** 用户本轮是否明确选择暂不补充发言。 */
    private Boolean userNoStatement;

    /** 是否由用户辅助律师默认代为发言。 */
    private Boolean advisorAutoSpeak;
}
