package com.legal.knowledge.service;

import com.legal.config.DocumentProcessingProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class SemanticDocumentChunker {

    private static final Pattern HEADING = Pattern.compile("^#{1,6}\\s+.+");
    private static final Pattern ORDERED_CLAUSE = Pattern.compile("^((第[一二三四五六七八九十百千万0-9]+条)|([0-9]+[.、）)]))\\s*.*");

    private final DocumentProcessingProperties properties;
    private final DocumentChunker fallbackChunker;

    public SemanticDocumentChunker(DocumentProcessingProperties properties, DocumentChunker fallbackChunker) {
        this.properties = properties;
        this.fallbackChunker = fallbackChunker;
    }

    public List<String> chunkMarkdown(String markdown) {
        if (!StringUtils.hasText(markdown)) {
            return List.of();
        }
        List<String> blocks = toBlocks(markdown);
        if (blocks.isEmpty()) {
            return fallbackChunker.chunk(markdown);
        }
        return mergeBlocks(blocks);
    }

    private List<String> toBlocks(String markdown) {
        List<String> blocks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        String[] lines = markdown.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        boolean inTable = false;
        for (String rawLine : lines) {
            String line = rawLine.stripTrailing();
            boolean tableLine = line.trim().startsWith("|") && line.trim().endsWith("|");
            boolean boundary = isBoundary(line, tableLine, inTable);
            if (boundary && !current.isEmpty()) {
                addBlock(blocks, current.toString());
                current.setLength(0);
            }
            if (StringUtils.hasText(line)) {
                current.append(line).append('\n');
            } else if (!current.isEmpty()) {
                addBlock(blocks, current.toString());
                current.setLength(0);
            }
            inTable = tableLine;
        }
        if (!current.isEmpty()) {
            addBlock(blocks, current.toString());
        }
        return blocks;
    }

    private boolean isBoundary(String line, boolean tableLine, boolean inTable) {
        String trimmed = line.trim();
        if (!StringUtils.hasText(trimmed)) {
            return false;
        }
        if (tableLine || inTable) {
            return false;
        }
        return HEADING.matcher(trimmed).matches() || ORDERED_CLAUSE.matcher(trimmed).matches();
    }

    private void addBlock(List<String> blocks, String value) {
        String normalized = value.trim();
        if (StringUtils.hasText(normalized)) {
            blocks.add(normalized);
        }
    }

    private List<String> mergeBlocks(List<String> blocks) {
        int maxChunkSize = Math.max(100, properties.getChunking().getMaxChunkSize());
        int minChunkSize = Math.max(1, properties.getChunking().getMinChunkSize());
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String block : blocks) {
            List<String> parts = block.length() > maxChunkSize ? splitOversizedBlock(block, maxChunkSize) : List.of(block);
            for (String part : parts) {
                if (current.isEmpty()) {
                    current.append(part);
                    continue;
                }
                int combinedLength = current.length() + 2 + part.length();
                if (combinedLength <= maxChunkSize || current.length() < minChunkSize) {
                    current.append("\n\n").append(part);
                } else {
                    addBlock(chunks, current.toString());
                    current.setLength(0);
                    current.append(part);
                }
            }
        }
        if (!current.isEmpty()) {
            addBlock(chunks, current.toString());
        }
        return chunks;
    }

    private List<String> splitOversizedBlock(String block, int maxChunkSize) {
        List<String> sentenceParts = splitByPreferredBoundary(block, maxChunkSize);
        if (!sentenceParts.isEmpty()) {
            return sentenceParts;
        }
        int overlap = Math.max(0, Math.min(properties.getChunking().getFallbackOverlap(), maxChunkSize / 2));
        return fallbackChunker.chunk(block, maxChunkSize, overlap);
    }

    private List<String> splitByPreferredBoundary(String block, int maxChunkSize) {
        List<String> result = new ArrayList<>();
        String[] units = block.split("(?<=[。！？；;!?])|\\n+");
        StringBuilder current = new StringBuilder();
        for (String unit : units) {
            String trimmed = unit.trim();
            if (!StringUtils.hasText(trimmed) || trimmed.length() > maxChunkSize) {
                return List.of();
            }
            if (current.length() + trimmed.length() + 1 > maxChunkSize) {
                addBlock(result, current.toString());
                current.setLength(0);
            }
            if (!current.isEmpty()) {
                current.append('\n');
            }
            current.append(trimmed);
        }
        if (!current.isEmpty()) {
            addBlock(result, current.toString());
        }
        return result;
    }
}
