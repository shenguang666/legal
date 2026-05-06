package com.legal.court.dto;

import com.legal.enums.CourtPartyRole;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 智能小法庭案件当事人 DTO。
 */
@Data
public class CourtPartyDto {

    /** 当事人主键ID。 */
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
