package com.legal.chat.cache;

import com.legal.chat.dto.CitationDto;
import lombok.Data;

import java.util.List;

/**
 * Redis 缓存的问答对象。
 *
 * <p>尽量保持与 AskResponse 一致的字段，便于直接回填响应与落库。</p>
 */
@Data
public class AnswerCachePayload {

    private String answer;
    private List<CitationDto> citations;
    private Double confidence;
    private String warning;

    private String sourceTraceId;
    private String sourceQuestion;
    private String normalizedQuestion;

    private String modelName;
    private Boolean knowledgeHit;
    private String hitChunkIds;

    private String cacheScope; // USER / TENANT
    private String cacheType;  // EXACT / SEMANTIC
    private Double similarityScore;
    private Long createdAt;
}
