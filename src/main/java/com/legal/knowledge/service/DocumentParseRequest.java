package com.legal.knowledge.service;

import com.legal.enums.DocumentParseMethod;
import org.springframework.web.multipart.MultipartFile;

public class DocumentParseRequest {

    private final MultipartFile file;
    private final DocumentParseMethod parseMethod;
    private final Integer chunkSize;
    private final Integer chunkOverlap;
    private final boolean cleaningEnabled;

    public DocumentParseRequest(MultipartFile file,
                                DocumentParseMethod parseMethod,
                                Integer chunkSize,
                                Integer chunkOverlap,
                                boolean cleaningEnabled) {
        this.file = file;
        this.parseMethod = parseMethod;
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
        this.cleaningEnabled = cleaningEnabled;
    }

    public MultipartFile getFile() {
        return file;
    }

    public DocumentParseMethod getParseMethod() {
        return parseMethod;
    }

    public Integer getChunkSize() {
        return chunkSize;
    }

    public Integer getChunkOverlap() {
        return chunkOverlap;
    }

    public boolean isCleaningEnabled() {
        return cleaningEnabled;
    }
}
