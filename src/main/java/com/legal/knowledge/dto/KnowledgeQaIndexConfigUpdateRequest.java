package com.legal.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 智能问答知识库索引配置更新请求。
 */
@Data
public class KnowledgeQaIndexConfigUpdateRequest {

    /** 目标智能问答知识库检索索引范围。 */
    @NotBlank(message = "索引范围不能为空")
    private String indexScope;
}
