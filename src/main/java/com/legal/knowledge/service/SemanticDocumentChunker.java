package com.legal.knowledge.service;

import com.legal.config.DocumentProcessingProperties;
import com.legal.enums.KbChunkType;
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
        return chunkMarkdown(markdown, properties.getMineru().getChunkSize());
    }

    public List<String> chunkMarkdown(String markdown, Integer maxChunkSizeOverride) {
        return chunkMarkdown(markdown, maxChunkSizeOverride, properties.getMineru().getMinChunkSize());
    }

    public List<String> chunkMarkdown(String markdown, Integer maxChunkSizeOverride, Integer minChunkSizeOverride) {
        return chunkMarkdownStructured(markdown, maxChunkSizeOverride, minChunkSizeOverride).stream()
                .filter(chunk -> chunk.chunkType() != KbChunkType.PARENT)
                .map(SemanticChunk::content)
                .toList();
    }

    public List<SemanticChunk> chunkMarkdownStructured(String markdown, Integer maxChunkSizeOverride, Integer minChunkSizeOverride) {
        if (!StringUtils.hasText(markdown)) {
            return List.of();
        }
        int maxChunkSize = resolveMaxChunkSize(maxChunkSizeOverride);
        List<String> blocks = toBlocks(markdown);
        if (blocks.isEmpty()) {
            return fallbackChunker.chunk(markdown, maxChunkSize, resolveFallbackOverlap(maxChunkSize)).stream()
                    .map(SemanticChunk::normal)
                    .toList();
        }
        boolean mergeEnabled = properties.getMineru().isMergeEnabled();
        int maxMergeSize = resolveMaxMergeSize(properties.getMineru().getMaxMergeSize());
        int minChunkSize = resolveMinChunkSize(maxMergeSize, minChunkSizeOverride);
        return mergeBlocksStructured(blocks, maxChunkSize, mergeEnabled, maxMergeSize, minChunkSize);
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

    private List<SemanticChunk> mergeBlocksStructured(List<String> blocks,
                                                      int maxChunkSize,
                                                      boolean mergeEnabled,
                                                      int maxMergeSize,
                                                      int minChunkSize) {
        List<SemanticChunk> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int parentGroup = 1;
        for (String block : blocks) {
            // chunk-size 只用于判断是否生成父子分块，以及控制子块切分大小。
            if (block.length() > maxChunkSize) {
                if (!current.isEmpty()) {
                    addNormalChunk(chunks, current.toString());
                    current.setLength(0);
                }
                int currentParentGroup = parentGroup++;
                chunks.add(SemanticChunk.parent(block, currentParentGroup));
                for (String child : splitOversizedBlockForMineru(block, maxChunkSize)) {
                    addChildChunk(chunks, child, currentParentGroup);
                }
                continue;
            }
            if (!mergeEnabled) {
                addNormalChunk(chunks, block);
                continue;
            }
            // 合并开关开启后，普通语义块使用 max-merge-size 控制相邻块合并大小。
            if (current.isEmpty()) {
                current.append(block);
                continue;
            }
            int combinedLength = current.length() + 2 + block.length();
            // min-chunk-size 仅在合并开启时生效，用于尽量避免生成过短、语义不完整的切片。
            if (combinedLength <= maxMergeSize || current.length() < minChunkSize) {
                current.append("\n\n").append(block);
            } else {
                addNormalChunk(chunks, current.toString());
                current.setLength(0);
                current.append(block);
            }
        }
        // 循环结束后将最后一个未写入的普通切片补充到结果集中。
        if (!current.isEmpty()) {
            addNormalChunk(chunks, current.toString());
        }
        return chunks;
    }

    private void addNormalChunk(List<SemanticChunk> chunks, String value) {
        String normalized = value.trim();
        if (StringUtils.hasText(normalized)) {
            chunks.add(SemanticChunk.normal(normalized));
        }
    }

    private void addChildChunk(List<SemanticChunk> chunks, String value, int parentGroup) {
        String normalized = value.trim();
        if (StringUtils.hasText(normalized)) {
            chunks.add(SemanticChunk.child(normalized, parentGroup));
        }
    }

    private List<String> splitOversizedBlockForMineru(String block, int maxChunkSize) {
        List<String> units = splitByNaturalBoundary(block);
        List<String> childUnits = new ArrayList<>();
        for (String unit : units) {
            if (unit.length() <= maxChunkSize) {
                childUnits.add(unit);
            } else {
                childUnits.addAll(splitLongUnitBySecondaryBoundary(unit, maxChunkSize));
            }
        }
        if (childUnits.isEmpty()) {
            childUnits = splitLongUnitBySecondaryBoundary(block, maxChunkSize);
        }
        return combineWithSemanticOverlap(childUnits, maxChunkSize);
    }

    private int resolveMaxChunkSize(Integer maxChunkSizeOverride) {
        return Math.max(100, maxChunkSizeOverride);
    }

    private int resolveMaxMergeSize(Integer maxMergeSizeOverride) {
        return Math.max(1, maxMergeSizeOverride);
    }

    private int resolveMinChunkSize(int maxMergeSize, Integer minChunkSizeOverride) {
        return Math.min(maxMergeSize, Math.max(1, minChunkSizeOverride));
    }

    private int resolveFallbackOverlap(int maxChunkSize) {
        return Math.max(0, Math.min(properties.getFallbackOverlap(), maxChunkSize / 2));
    }

    private List<String> splitByNaturalBoundary(String block) {
        String[] units = block.split("(?<=[。！？；;!?])|\\n+");
        List<String> result = new ArrayList<>();
        for (String unit : units) {
            String trimmed = unit.trim();
            if (StringUtils.hasText(trimmed)) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private List<String> splitLongUnitBySecondaryBoundary(String unit, int maxChunkSize) {
        List<String> result = new ArrayList<>();
        String[] parts = unit.split("(?<=[，,、：:（）()])|\\n+");
        StringBuilder current = new StringBuilder();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!StringUtils.hasText(trimmed)) {
                continue;
            }
            if (trimmed.length() > maxChunkSize) {
                if (!current.isEmpty()) {
                    addBlock(result, current.toString());
                    current.setLength(0);
                }
                result.addAll(fallbackChunker.chunk(trimmed, maxChunkSize, 0));
                continue;
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

    private List<String> combineWithSemanticOverlap(List<String> units, int maxChunkSize) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        String previousTail = "";
        int maxOverlapChars = Math.max(0, (int) Math.floor(maxChunkSize * 0.15d));
        for (String unit : units) {
            if (!StringUtils.hasText(unit)) {
                continue;
            }
            String candidate = current.isEmpty() ? unit : current + "\n" + unit;
            if (candidate.length() <= maxChunkSize) {
                if (!current.isEmpty()) {
                    current.append('\n');
                }
                current.append(unit);
                continue;
            }
            if (!current.isEmpty()) {
                String chunk = current.toString();
                result.add(chunk);
                previousTail = resolveOverlapTail(chunk, maxOverlapChars);
                current.setLength(0);
            }
            if (StringUtils.hasText(previousTail) && previousTail.length() + 1 + unit.length() <= maxChunkSize) {
                current.append(previousTail).append('\n');
            }
            current.append(unit);
        }
        if (!current.isEmpty()) {
            result.add(current.toString().trim());
        }
        return result;
    }

    private String resolveOverlapTail(String chunk, int maxOverlapChars) {
        if (maxOverlapChars <= 0 || !StringUtils.hasText(chunk)) {
            return "";
        }
        List<String> units = splitByNaturalBoundary(chunk);
        StringBuilder tail = new StringBuilder();
        for (int i = units.size() - 1; i >= 0; i--) {
            String unit = units.get(i);
            int candidateLength = tail.isEmpty() ? unit.length() : unit.length() + 1 + tail.length();
            if (candidateLength > maxOverlapChars) {
                break;
            }
            if (tail.isEmpty()) {
                tail.insert(0, unit);
            } else {
                tail.insert(0, unit + "\n");
            }
        }
        return tail.toString().trim();
    }
}
