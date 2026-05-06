package com.legal.court.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 智能小法庭庭审证据上下文。
 */
@Data
public class CourtEvidenceContext {

    /** 注入大模型的父级语义块条目列表。 */
    private List<Item> items = new ArrayList<>();
    /** 因父分块缺失而被跳过的子分块ID列表。 */
    private List<Long> skippedChildChunkIds = new ArrayList<>();

    /**
     * 单条父级上下文条目。
     */
    @Data
    public static class Item {

        /** 父分块ID。 */
        private Long parentChunkId;
        /** 命中时的子分块ID或普通分块ID。 */
        private Long hitChildChunkId;
        /** 所属文档ID。 */
        private Long documentId;
        /** 展示文本。 */
        private String content;
        /** 来源描述。 */
        private String source;
    }
}
