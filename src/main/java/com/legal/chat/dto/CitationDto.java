package com.legal.chat.dto;

public class CitationDto {

    private Long documentId;
    private String source;
    private String fragment;

    public CitationDto() {
    }

    public CitationDto(Long documentId, String source, String fragment) {
        this.documentId = documentId;
        this.source = source;
        this.fragment = fragment;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
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
}
