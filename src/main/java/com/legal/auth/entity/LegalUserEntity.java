package com.legal.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户表实体。
 */
@Data
@TableName("legal_user")
public class LegalUserEntity {

    /** 用户主键ID。 */
    @TableId(value = "user_id", type = IdType.AUTO)
    private Long userId;
    /** 租户ID。 */
    private Long tenantId;
    /** 登录用户名。 */
    private String username;
    /** 密码摘要（SHA-256）。 */
    private String passwordHash;
    /** 显示名称。 */
    private String displayName;
    /** 角色编码（ADMIN/USER）。 */
    private String roleCode;
    /** 用户状态（ACTIVE/INACTIVE）。 */
    private String status;
    /** 最后登录时间。 */
    private LocalDateTime lastLoginAt;
    /** 创建时间。 */
    private LocalDateTime createdAt;
    /** 更新时间。 */
    private LocalDateTime updatedAt;
}
