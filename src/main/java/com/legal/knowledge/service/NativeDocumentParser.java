package com.legal.knowledge.service;

import com.legal.enums.DocumentParseMethod;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NativeDocumentParser implements DocumentParser {

    private final DocumentTextExtractor documentTextExtractor;
    private final DocumentChunker documentChunker;

    public NativeDocumentParser(DocumentTextExtractor documentTextExtractor, DocumentChunker documentChunker) {
        this.documentTextExtractor = documentTextExtractor;
        this.documentChunker = documentChunker;
    }

    @Override
    public DocumentParseMethod method() {
        return DocumentParseMethod.NATIVE;
    }

    @Override
    public DocumentParseResult parse(DocumentParseRequest request) {
        String extractedText = documentTextExtractor.extract(request.getFile());
        List<String> chunks = buildChunks(extractedText, request.getChunkSize(), request.getChunkOverlap());
        return new DocumentParseResult(method(), extractedText, null, chunks, null, null, null);
    }

    public List<String> buildChunks(String extractedText, Integer chunkSize, Integer chunkOverlap) {
        if (chunkSize == null) {
            return documentChunker.chunk(extractedText);
        }
        if (chunkOverlap == null) {
            int overlap = (int) Math.round(chunkSize * 0.15d);
            return documentChunker.chunk(extractedText, chunkSize, overlap);
        }
        return documentChunker.chunk(extractedText, chunkSize, chunkOverlap);
    }
}
