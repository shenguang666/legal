package com.legal.knowledge.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DocumentCleaningReport {

    private final int originalChars;
    private int cleanedChars;
    private int removedLineCount;
    private int removedChunkCount;
    private final Map<String, Integer> reasonSummary = new LinkedHashMap<>();
    private final List<String> removedSamples = new ArrayList<>();

    public DocumentCleaningReport(int originalChars) {
        this.originalChars = Math.max(0, originalChars);
    }

    public int getOriginalChars() {
        return originalChars;
    }

    public int getCleanedChars() {
        return cleanedChars;
    }

    public void setCleanedChars(int cleanedChars) {
        this.cleanedChars = Math.max(0, cleanedChars);
    }

    public int getRemovedLineCount() {
        return removedLineCount;
    }

    public int getRemovedChunkCount() {
        return removedChunkCount;
    }

    public Map<String, Integer> getReasonSummary() {
        return reasonSummary;
    }

    public List<String> getRemovedSamples() {
        return removedSamples;
    }

    public boolean hasRemovedContent() {
        return removedLineCount > 0 || removedChunkCount > 0;
    }

    public void addRemovedLine(String reason, String content, int sampleLimit, int sampleMaxChars) {
        removedLineCount++;
        addRemoved(reason, content, sampleLimit, sampleMaxChars);
    }

    public void addRemovedChunk(String reason, String content, int sampleLimit, int sampleMaxChars) {
        removedChunkCount++;
        addRemoved(reason, content, sampleLimit, sampleMaxChars);
    }

    public void merge(DocumentCleaningReport other, int sampleLimit, int sampleMaxChars) {
        if (other == null) {
            return;
        }
        this.removedLineCount += other.removedLineCount;
        this.removedChunkCount += other.removedChunkCount;
        other.reasonSummary.forEach((reason, count) -> this.reasonSummary.merge(reason, count, Integer::sum));
        for (String sample : other.removedSamples) {
            if (this.removedSamples.size() >= sampleLimit) {
                break;
            }
            this.removedSamples.add(truncate(sample, sampleMaxChars));
        }
    }

    private void addRemoved(String reason, String content, int sampleLimit, int sampleMaxChars) {
        String safeReason = reason == null || reason.isBlank() ? "UNKNOWN" : reason;
        reasonSummary.merge(safeReason, 1, Integer::sum);
        if (removedSamples.size() < sampleLimit && content != null && !content.isBlank()) {
            removedSamples.add("[" + safeReason + "] " + truncate(content.strip(), sampleMaxChars));
        }
    }

    private String truncate(String value, int sampleMaxChars) {
        int max = Math.max(20, sampleMaxChars);
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max) + "...";
    }
}
