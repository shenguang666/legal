package com.legal.knowledge.service;

public class DocumentCleaningResult {

    private final String content;
    private final DocumentCleaningReport report;

    public DocumentCleaningResult(String content, DocumentCleaningReport report) {
        this.content = content == null ? "" : content;
        this.report = report;
    }

    public String getContent() {
        return content;
    }

    public DocumentCleaningReport getReport() {
        return report;
    }
}
