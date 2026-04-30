package com.legal.memory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.legal.enums.UserMemoryStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户长期记忆事实表实体。
 */
@Data
@TableName("user_memory_item")
public class UserMemoryItemEntity {

    /** 记忆主键ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 租户ID。 */
    private Long tenantId;
    /** 用户ID。 */
    private Long userId;
    /** 记忆类型，如 identity / profession / emotion。 */
    private String memoryType;
    /** 记忆键。 */
    private String memoryKey;
    /** 记忆值。 */
    private String memoryValue;
    /** 敏感等级。 */
    private String sensitivityLevel;
    /** 置信度。 */
    private BigDecimal confidence;
    /** 连续命中确认次数。 */
    private Integer confirmationCount;
    /** 稳定升级阈值。 */
    private Integer stableThreshold;
    /** 状态。 */
    private UserMemoryStatus status;
    /** 来源会话ID。 */
    private String sourceSessionId;
    /** 来源消息ID。 */
    private Long sourceMessageId;
    /** 是否由用户确认。 */
    private Boolean confirmedByUser;
    /** 过期时间。 */
    private LocalDateTime expiresAt;
    /** 最近观察时间。 */
    private LocalDateTime lastSeenAt;
    /** 稳定生效时间。 */
    private LocalDateTime stableSince;
    /** 模型版本。 */
    private String modelVersion;
    /** Prompt 版本。 */
    private String promptVersion;
    /** 版本号。 */
    private Integer version;
    /** 创建时间。 */
    private LocalDateTime createdAt;
    /** 更新时间。 */
    private LocalDateTime updatedAt;
}
