package com.legal.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天快捷问题热词表实体。
 */
@Data
@TableName("chat_hotword")
public class ChatHotwordEntity {

    /** 热词主键ID。 */
    @TableId(value = "hotword_id", type = IdType.AUTO)
    private Long hotwordId;
    /** 租户ID，用于隔离不同租户的热词池。 */
    private Long tenantId;
    /** 热词标记，用于前端提交 askStream 时标识命中的热词。 */
    private String hotwordKey;
    /** 热词内容，展示在聊天页快捷问题区域。 */
    private String content;
    /** 预设答案，命中热词标记时由后端直接返回。 */
    private String presetAnswer;
    /** 热词分类，如劳动合同、合同审查、报销合规。 */
    private String category;
    /** 热词权重，用于管理页排序和后续推荐扩展。 */
    private Integer weight;
    /** 热词排序号，数值越小越靠前。 */
    private Integer sortOrder;
    /** 是否启用，true 启用、false 停用。 */
    private Boolean enabled;
    /** 是否逻辑删除，true 已删除、false 未删除。 */
    private Boolean deleted;
    /** 创建用户ID。 */
    private Long createdBy;
    /** 最近更新用户ID。 */
    private Long updatedBy;
    /** 创建时间。 */
    private LocalDateTime createdAt;
    /** 更新时间。 */
    private LocalDateTime updatedAt;
}
