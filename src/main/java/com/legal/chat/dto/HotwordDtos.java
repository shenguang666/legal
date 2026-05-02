package com.legal.chat.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

public final class HotwordDtos {

    private HotwordDtos() {
    }

    @Data
    public static class Item {
        private Long hotwordId;
        private String hotwordKey;
        private String content;
        private String presetAnswer;
        private String category;
        private Integer weight;
        private Integer sortOrder;
        private Boolean enabled;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    public static class RandomItem {
        private Long hotwordId;
        private String hotwordKey;
        private String content;
        private String category;
    }

    @Data
    public static class PageResult {
        private long total;
        private int pageNo;
        private int pageSize;
        private List<Item> items;
    }
}
