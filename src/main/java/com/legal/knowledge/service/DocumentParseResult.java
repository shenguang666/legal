package com.legal.knowledge.service;

import com.legal.enums.DocumentParseMethod;

import java.util.List;

public class DocumentParseResult {

    private final DocumentParseMethod parseMethod;
    private final String text;
    private final String markdown;
    private final List<String> chunks;
    private final String mineruBatchId;
    private final String mineruDataId;
    private final String mineruFullZipUrl;

    public DocumentParseResult(DocumentParseMethod parseMethod,
                               String text,
                               String markdown,
                               List<String> chunks,
                               String mineruBatchId,
                               String mineruDataId,
                               String mineruFullZipUrl) {
        this.parseMethod = parseMethod;
        this.text = text;
        this.markdown = markdown;
        this.chunks = chunks == null ? List.of() : List.copyOf(chunks);
        this.mineruBatchId = mineruBatchId;
        this.mineruDataId = mineruDataId;
        this.mineruFullZipUrl = mineruFullZipUrl;
    }

    public DocumentParseMethod getParseMethod() {
        return parseMethod;
    }

    public String getText() {
        return text;
    }

    public String getMarkdown() {
        return markdown;
    }

    public List<String> getChunks() {
        return chunks;
    }

    public String getMineruBatchId() {
        return mineruBatchId;
    }

    public String getMineruDataId() {
        return mineruDataId;
    }

    public String getMineruFullZipUrl() {
        return mineruFullZipUrl;
    }
}
