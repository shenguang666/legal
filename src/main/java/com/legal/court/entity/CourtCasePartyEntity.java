package com.legal.court.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.legal.enums.CourtPartyRole;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 智能小法庭案件当事人实体。
 */
@Data
@TableName("court_case_party")
public class CourtCasePartyEntity {

    /** 当事人主键ID。 */
    @TableId(value = "party_id", type = IdType.AUTO)
    private Long partyId;
    /** 租户ID。 */
    private Long tenantId;
    /** 案件ID。 */
    private Long caseId;
    /** 当事人角色。 */
    private CourtPartyRole partyRole;
    /** 当事人显示名称。 */
    private String displayName;
    /** 当事人简介或主体信息。 */
    private String description;
    /** 是否为用户所代表的一方。 */
    private Boolean userSide;
    /** 创建时间。 */
    private LocalDateTime createdAt;
    /** 更新时间。 */
    private LocalDateTime updatedAt;
}
