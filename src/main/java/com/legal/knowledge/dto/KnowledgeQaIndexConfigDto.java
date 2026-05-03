package com.legal.knowledge.dto;

import lombok.Data;

import java.util.List;

/**
 * 智能问答知识库索引配置响应。
 */
@Data
public class KnowledgeQaIndexConfigDto {

    /** 当前智能问答知识库检索索引范围。 */
    private String indexScope;
    /** 可选择的智能问答知识库检索索引范围。 */
    private List<String> availableScopes;
    /** 原生解析知识库索引名称。 */
    private String nativeIndexName;
    /** MinerU 精准解析知识库索引名称。 */
    private String mineruIndexName;
}
