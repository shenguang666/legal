package com.legal.knowledge.dto;

import java.util.List;

public class DocumentProcessingCapabilitiesDto {

    private String defaultParseMethod;
    private int maxUploadDocuments;
    private List<String> availableParseMethods;
    private boolean cleaningAvailable;

    public String getDefaultParseMethod() {
        return defaultParseMethod;
    }

    public void setDefaultParseMethod(String defaultParseMethod) {
        this.defaultParseMethod = defaultParseMethod;
    }

    public int getMaxUploadDocuments() {
        return maxUploadDocuments;
    }

    public void setMaxUploadDocuments(int maxUploadDocuments) {
        this.maxUploadDocuments = maxUploadDocuments;
    }

    public List<String> getAvailableParseMethods() {
        return availableParseMethods;
    }

    public void setAvailableParseMethods(List<String> availableParseMethods) {
        this.availableParseMethods = availableParseMethods;
    }

    public boolean isCleaningAvailable() {
        return cleaningAvailable;
    }

    public void setCleaningAvailable(boolean cleaningAvailable) {
        this.cleaningAvailable = cleaningAvailable;
    }
}
