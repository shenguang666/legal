package com.legal.feedback.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 问答反馈表实体。
 */
@Data
@TableName("qa_feedback")
public class QaFeedbackEntity {

    /** 反馈主键ID。 */
    @TableId(value = "feedback_id", type = IdType.AUTO)
    private Long feedbackId;
    /** 消息ID。 */
    private Long messageId;
    /** 是否有帮助。 */
    private Boolean helpful;
    /** 反馈补充说明。 */
    private String comment;
    /** 创建时间。 */
    private LocalDateTime createdAt;
}
