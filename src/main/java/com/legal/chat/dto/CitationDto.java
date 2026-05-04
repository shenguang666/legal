package com.legal.chat.dto;

import java.util.ArrayList;
import java.util.List;

public class CitationDto {

    /** 命中文档ID。 */
    private Long documentId;
    /** 命中切片ID。 */
    private Long chunkId;
    /** 引用来源。 */
    private String source;
    /** 引用文本片段。 */
    private String fragment;
    /** 引用关联的图片证据列表。 */
    private List<CitationImageDto> images = new ArrayList<>();

    public CitationDto() {
    }

    public CitationDto(Long documentId, String source, String fragment) {
        this(documentId, null, source, fragment, List.of());
    }

    public CitationDto(Long documentId, Long chunkId, String source, String fragment, List<CitationImageDto> images) {
        this.documentId = documentId;
        this.chunkId = chunkId;
        this.source = source;
        this.fragment = fragment;
        this.images = images == null ? new ArrayList<>() : new ArrayList<>(images);
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public Long getChunkId() {
        return chunkId;
    }

    public void setChunkId(Long chunkId) {
        this.chunkId = chunkId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getFragment() {
        return fragment;
    }

    public void setFragment(String fragment) {
        this.fragment = fragment;
    }

    public List<CitationImageDto> getImages() {
        if (images == null) {
            images = new ArrayList<>();
        }
        return images;
    }

    public void setImages(List<CitationImageDto> images) {
        this.images = images == null ? new ArrayList<>() : new ArrayList<>(images);
    }
}
