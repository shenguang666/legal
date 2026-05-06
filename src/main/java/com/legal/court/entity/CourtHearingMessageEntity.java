package com.legal.court.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.legal.enums.CourtArgumentSpeaker;
import com.legal.enums.CourtPartyRole;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 智能小法庭庭审消息实体。
 */
@Data
@TableName("court_hearing_message")
public class CourtHearingMessageEntity {

    /** 庭审消息主键ID。 */
    @TableId(value = "message_id", type = IdType.AUTO)
    private Long messageId;
    /** 租户ID。 */
    private Long tenantId;
    /** 案件ID。 */
    private Long caseId;
    /** 所属庭审轮次ID。 */
    private Long roundId;
    /** 所属轮次的尝试编号。 */
    private Integer attemptId;
    /** 发言角色。 */
    private CourtArgumentSpeaker speakerRole;
    /** 发言所代表的当事人角色。 */
    private CourtPartyRole speakerParty;
    /** 消息类型。 */
    private String messageType;
    /** 消息文本内容或结构化 JSON 内容。 */
    private String content;
    /** 本条消息消耗的输入 token 数。 */
    private Integer tokenInput;
    /** 本条消息消耗的输出 token 数。 */
    private Integer tokenOutput;
    /** 创建时间。 */
    private LocalDateTime createdAt;
}
