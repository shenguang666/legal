package com.legal.knowledge.service;

import com.legal.enums.DocumentParseMethod;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NativeDocumentParser implements DocumentParser {

    private final DocumentTextExtractor documentTextExtractor;
    private final DocumentChunker documentChunker;
    private final DocumentContentCleaner documentContentCleaner;

    public NativeDocumentParser(DocumentTextExtractor documentTextExtractor,
                                DocumentChunker documentChunker,
                                DocumentContentCleaner documentContentCleaner) {
        this.documentTextExtractor = documentTextExtractor;
        this.documentChunker = documentChunker;
        this.documentContentCleaner = documentContentCleaner;
    }

    @Override
    public DocumentParseMethod method() {
        return DocumentParseMethod.NATIVE;
    }

    @Override
    public DocumentParseResult parse(DocumentParseRequest request) {
        String extractedText = documentTextExtractor.extract(request.getFile());
        DocumentCleaningResult cleaningResult = request.isCleaningEnabled() ? documentContentCleaner.cleanText(extractedText) : null;
        String chunkSource = cleaningResult == null ? extractedText : cleaningResult.getContent();
        List<String> chunks = buildChunks(chunkSource, request.getChunkSize(), request.getChunkOverlap());
        return new DocumentParseResult(method(), chunkSource, null, chunks,
                cleaningResult == null ? null : cleaningResult.getReport(), null, null, null);
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
