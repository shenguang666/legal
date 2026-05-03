package com.legal.knowledge.service;

import com.legal.config.DocumentProcessingProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class DocumentContentCleaner {

    private static final Pattern PAGE_NUMBER = Pattern.compile("^(第\\s*)?\\d+\\s*(页)?(\\s*[/／\\-]\\s*(共\\s*)?\\d+\\s*(页)?)?$|^page\\s+\\d+(\\s+of\\s+\\d+)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern COPYRIGHT = Pattern.compile(".*(版权所有|版权归|copyright|all rights reserved|©|®).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern EXPORT_METADATA = Pattern.compile(".*(官网导出|系统导出|导出时间|下载时间|打印时间|生成时间|来源[:：]|来自[:：]|文档编号[:：]|仅供内部使用).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern STANDALONE_URL = Pattern.compile("^(https?://|www\\.)\\S+$", Pattern.CASE_INSENSITIVE);
    private static final Pattern LEGAL_HEADING = Pattern.compile("^(#{1,6}\\s*)?((第[一二三四五六七八九十百千万0-9]+[章节条款])|([0-9]+[.、）)]))?\\s*(违约责任|争议解决|付款条款|支付条款|保密义务|合同解除|知识产权|不可抗力|适用法律|生效条件|验收标准|质量保证|交付义务|赔偿责任|法律责任|风险提示)\\s*$");
    private static final Pattern ORDERED_CLAUSE = Pattern.compile("^((第[一二三四五六七八九十百千万0-9]+条)|([0-9]+[.、）)]))\\s*\\S+.*");
    private static final Pattern MARKDOWN_HEADING = Pattern.compile("^#{1,6}\\s+\\S+.*");
    private static final Pattern TABLE_LINE = Pattern.compile("^\\s*\\|.*\\|\\s*$");

    private final DocumentProcessingProperties properties;

    public DocumentContentCleaner(DocumentProcessingProperties properties) {
        this.properties = properties;
    }

    public DocumentCleaningResult cleanText(String text) {
        return clean(text, false);
    }

    public DocumentCleaningResult cleanMarkdown(String markdown) {
        return clean(markdown, true);
    }

    private DocumentCleaningResult clean(String content, boolean markdown) {
        String source = content == null ? "" : content;
        DocumentCleaningReport report = new DocumentCleaningReport(source.length());
        String normalized = normalize(source, markdown);
        List<String> lines = List.of(normalized.split("\\n", -1));
        Set<Integer> repeatedNoiseIndexes = repeatedHeaderFooterIndexes(lines);
        List<String> kept = new ArrayList<>();
        DocumentProcessingProperties.Cleaning cleaning = properties.getCleaning();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String trimmed = line.trim();
            if (!StringUtils.hasText(trimmed)) {
                appendBlankLine(kept);
                continue;
            }
            String reason = removalReason(trimmed, markdown, repeatedNoiseIndexes.contains(i));
            if (reason != null) {
                report.addRemovedLine(reason, trimmed, cleaning.getRemovedSampleLimit(), cleaning.getRemovedSampleMaxChars());
                continue;
            }
            kept.add(line.stripTrailing());
        }
        String cleaned = collapseBlankLines(String.join("\n", kept)).trim();
        cleaned = cleaned.replaceAll("(?<=[\\u4E00-\\u9FFF])[ ]+(?=[\\u4E00-\\u9FFF])", "");
        report.setCleanedChars(cleaned.length());
        return new DocumentCleaningResult(cleaned, report);
    }

    public boolean isLowQualityChunk(String chunk) {
        if (!StringUtils.hasText(chunk)) {
            return true;
        }
        String trimmed = chunk.trim();
        if (isProtectedLine(trimmed, true)) {
            return false;
        }
        String compact = trimmed.replaceAll("\\s+", " ");
        if (removalReason(compact, true, false) != null) {
            return true;
        }
        int effectiveLength = compact.replaceAll("[\\p{Punct}\\p{IsPunctuation}\\s]", "").length();
        return effectiveLength < Math.max(1, properties.getCleaning().getMinEffectiveChunkLength());
    }

    private String normalize(String source, boolean markdown) {
        String normalized = source
                .replace('\u0000', ' ')
                .replace('\u00A0', ' ')
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace("\f", "\n\f\n")
                .replaceAll("[\\t\\x0B]+", " ")
                .replaceAll("[ ]{2,}", " ");
        if (!markdown) {
            normalized = normalized.replaceAll("\\n{3,}", "\n\n");
        }
        return normalized;
    }

    private Set<Integer> repeatedHeaderFooterIndexes(List<String> lines) {
        Set<Integer> indexes = new HashSet<>();
        DocumentProcessingProperties.Cleaning cleaning = properties.getCleaning();
        if (!cleaning.isRemoveRepeatedHeaderFooter()) {
            return indexes;
        }
        List<List<Integer>> pages = splitPageLineIndexes(lines);
        if (pages.size() < Math.max(2, cleaning.getRepeatedLineMinOccurrences())) {
            return indexes;
        }
        Map<String, List<Integer>> occurrences = new HashMap<>();
        int scanLines = Math.max(1, cleaning.getHeaderFooterScanLines());
        for (List<Integer> page : pages) {
            Set<Integer> candidates = new HashSet<>();
            for (int i = 0; i < page.size() && i < scanLines; i++) {
                candidates.add(page.get(i));
            }
            for (int i = Math.max(0, page.size() - scanLines); i < page.size(); i++) {
                candidates.add(page.get(i));
            }
            for (Integer index : candidates) {
                if (!looksLikeRepeatedHeaderFooterCandidate(lines.get(index))) {
                    continue;
                }
                String key = normalizeRepeatKey(lines.get(index));
                if (StringUtils.hasText(key)) {
                    occurrences.computeIfAbsent(key, ignored -> new ArrayList<>()).add(index);
                }
            }
        }
        int minOccurrences = Math.max(2, cleaning.getRepeatedLineMinOccurrences());
        int minPages = (int) Math.ceil(pages.size() * Math.max(0.1d, Math.min(1.0d, cleaning.getRepeatedLinePageRatio())));
        for (Map.Entry<String, List<Integer>> entry : occurrences.entrySet()) {
            String key = entry.getKey();
            List<Integer> matched = entry.getValue();
            if (matched.size() >= minOccurrences && matched.size() >= minPages && key.length() <= cleaning.getRepeatedLineMaxLength()) {
                indexes.addAll(matched);
            }
        }
        return indexes;
    }

    private List<List<Integer>> splitPageLineIndexes(List<String> lines) {
        List<List<Integer>> pages = new ArrayList<>();
        List<Integer> current = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).indexOf('\f') >= 0) {
                addPage(pages, current);
                current = new ArrayList<>();
                continue;
            }
            if (StringUtils.hasText(lines.get(i))) {
                current.add(i);
            }
        }
        addPage(pages, current);
        return pages;
    }

    private void addPage(List<List<Integer>> pages, List<Integer> current) {
        if (!current.isEmpty()) {
            pages.add(current);
        }
    }

    private String normalizeRepeatKey(String line) {
        if (!StringUtils.hasText(line)) {
            return "";
        }
        return line.trim()
                .replaceAll("\\d+", "#")
                .replaceAll("[ ]{2,}", " ")
                .toLowerCase(Locale.ROOT);
    }

    private boolean looksLikeRepeatedHeaderFooterCandidate(String line) {
        String trimmed = line == null ? "" : line.trim();
        if (!StringUtils.hasText(trimmed)) {
            return false;
        }
        if (trimmed.matches(".*[。；！？!?；]$")) {
            return false;
        }
        return trimmed.length() <= Math.max(10, properties.getCleaning().getRepeatedLineMaxLength());
    }

    private String removalReason(String trimmed, boolean markdown, boolean repeatedHeaderFooter) {
        DocumentProcessingProperties.Cleaning cleaning = properties.getCleaning();
        if (isProtectedLine(trimmed, markdown)) {
            return null;
        }
        if (repeatedHeaderFooter) {
            return "REPEATED_HEADER_FOOTER";
        }
        if (cleaning.isRemovePageNumbers() && PAGE_NUMBER.matcher(trimmed).matches()) {
            return "PAGE_NUMBER";
        }
        if (cleaning.isRemoveCopyrightLines() && COPYRIGHT.matcher(trimmed).matches()) {
            return "COPYRIGHT";
        }
        if (cleaning.isRemoveExportMetadata() && EXPORT_METADATA.matcher(trimmed).matches()) {
            return "EXPORT_METADATA";
        }
        if (cleaning.isRemoveStandaloneUrls() && STANDALONE_URL.matcher(trimmed).matches()) {
            return "STANDALONE_URL";
        }
        return null;
    }

    private boolean isProtectedLine(String trimmed, boolean markdown) {
        if (!StringUtils.hasText(trimmed)) {
            return false;
        }
        return LEGAL_HEADING.matcher(trimmed).matches()
                || ORDERED_CLAUSE.matcher(trimmed).matches()
                || (markdown && (MARKDOWN_HEADING.matcher(trimmed).matches() || TABLE_LINE.matcher(trimmed).matches()));
    }

    private void appendBlankLine(List<String> kept) {
        if (!kept.isEmpty() && StringUtils.hasText(kept.get(kept.size() - 1))) {
            kept.add("");
        }
    }

    private String collapseBlankLines(String value) {
        return value.replaceAll("\\n{3,}", "\n\n");
    }
}
