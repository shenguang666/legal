package com.legal.court.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.legal.enums.CourtGraphEventStatus;
import com.legal.enums.CourtGraphEventType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 智能小法庭图谱事件实体。
 */
@Data
@TableName("court_graph_event")
public class CourtGraphEventEntity {

    /** 图谱事件主键ID。 */
    @TableId(value = "event_id", type = IdType.AUTO)
    private Long eventId;
    /** 租户ID。 */
    private Long tenantId;
    /** 案件ID。 */
    private Long caseId;
    /** 产生该事件的庭审轮次ID。 */
    private Long roundId;
    /** 事件类型。 */
    private CourtGraphEventType eventType;
    /** 事件载荷 JSON，包含节点/关系类型、业务ID、属性、关系两端等。 */
    private String payloadJson;
    /** 投影状态。 */
    private CourtGraphEventStatus status;
    /** 当前事件投影失败重试次数。 */
    private Integer retryCount;
    /** 最近一次投影失败原因。 */
    private String errorMessage;
    /** 下次允许重试时间。 */
    private LocalDateTime nextRetryAt;
    /** 事件被成功投影的时间。 */
    private LocalDateTime appliedAt;
    /** 创建时间。 */
    private LocalDateTime createdAt;
    /** 更新时间。 */
    private LocalDateTime updatedAt;
}
