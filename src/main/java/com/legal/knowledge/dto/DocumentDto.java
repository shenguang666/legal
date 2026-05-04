package com.legal.knowledge.dto;

import java.time.LocalDateTime;

public class DocumentDto {

    private Long documentId;
    private Long ownerUserId;
    private String ownerUsername;
    private String title;
    private String source;
    private String bizType;
    private String status;
    private String indexStatus;
    private String parseMethod;
    private String parseStatus;
    private String parseFailureReason;
    private Boolean cleaningEnabled;
    private String documentUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getBizType() {
        return bizType;
    }

    public void setBizType(String bizType) {
        this.bizType = bizType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getIndexStatus() {
        return indexStatus;
    }

    public void setIndexStatus(String indexStatus) {
        this.indexStatus = indexStatus;
    }

    public String getParseMethod() {
        return parseMethod;
    }

    public void setParseMethod(String parseMethod) {
        this.parseMethod = parseMethod;
    }

    public String getParseStatus() {
        return parseStatus;
    }

    public void setParseStatus(String parseStatus) {
        this.parseStatus = parseStatus;
    }

    public String getParseFailureReason() {
        return parseFailureReason;
    }

    public void setParseFailureReason(String parseFailureReason) {
        this.parseFailureReason = parseFailureReason;
    }

    public Boolean getCleaningEnabled() {
        return cleaningEnabled;
    }

    public void setCleaningEnabled(Boolean cleaningEnabled) {
        this.cleaningEnabled = cleaningEnabled;
    }

    public String getDocumentUrl() {
        return documentUrl;
    }

    public void setDocumentUrl(String documentUrl) {
        this.documentUrl = documentUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
