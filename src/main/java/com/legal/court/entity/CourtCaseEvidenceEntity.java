package com.legal.court.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.legal.enums.CourtEvidenceRole;
import com.legal.enums.CourtEvidenceStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 智能小法庭案件证据登记实体。
 */
@Data
@TableName("court_case_evidence")
public class CourtCaseEvidenceEntity {

    /** 证据登记主键ID。 */
    @TableId(value = "evidence_id", type = IdType.AUTO)
    private Long evidenceId;
    /** 租户ID。 */
    private Long tenantId;
    /** 案件ID。 */
    private Long caseId;
    /** 关联的知识库或风险规则文档ID。 */
    private Long documentId;
    /** 证据角色。 */
    private CourtEvidenceRole evidenceRole;
    /** 证据展示名称。 */
    private String displayName;
    /** 证据用途描述。 */
    private String description;
    /** 证据状态。 */
    private CourtEvidenceStatus status;
    /** 创建时间。 */
    private LocalDateTime createdAt;
    /** 更新时间。 */
    private LocalDateTime updatedAt;
}
