package com.legal.chat.dto;

/**
 * RAG 引用中的图片证据 DTO。
 */
public class CitationImageDto {

    /** 图片资产ID。 */
    private Long imageAssetId;
    /** 图片访问 URL。 */
    private String url;
    /** 图片描述。 */
    private String description;
    /** MinerU 结果包内原始图片路径。 */
    private String originalPath;
    /** 图片在切片中的出现顺序。 */
    private Integer order;

    public CitationImageDto() {
    }

    public CitationImageDto(Long imageAssetId, String url, String description, String originalPath, Integer order) {
        this.imageAssetId = imageAssetId;
        this.url = url;
        this.description = description;
        this.originalPath = originalPath;
        this.order = order;
    }

    public Long getImageAssetId() {
        return imageAssetId;
    }

    public void setImageAssetId(Long imageAssetId) {
        this.imageAssetId = imageAssetId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOriginalPath() {
        return originalPath;
    }

    public void setOriginalPath(String originalPath) {
        this.originalPath = originalPath;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }
}
