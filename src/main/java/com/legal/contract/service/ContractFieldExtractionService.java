package com.legal.contract.service;

import com.legal.config.ContractReviewProperties;
import com.legal.contract.entity.ContractFieldDefinitionEntity;
import com.legal.enums.ContractFieldStatus;
import com.legal.knowledge.entity.KbChunkEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ContractFieldExtractionService {

    private final ContractReviewProperties properties;
    private final ContractFieldDefinitionService fieldDefinitionService;

    public ContractFieldExtractionService(ContractReviewProperties properties,
                                           ContractFieldDefinitionService fieldDefinitionService) {
        this.properties = properties;
        this.fieldDefinitionService = fieldDefinitionService;
    }

    public List<ExtractedContractField> extract(Long tenantId, List<KbChunkEntity> chunks) {
        List<ExtractedContractField> fields = new ArrayList<>();
        int[] counter = {1};
        for (ContractFieldDefinitionEntity definition : fieldDefinitionService.listEnabledDefinitions(tenantId)) {
            List<Candidate> candidates = collectCandidates(chunks, definition);
            String extractorType = extractorTypeLabel(definition.getExtractorKind());
            if (Boolean.TRUE.equals(definition.getRepeatable())) {
                fields.addAll(resolveRepeatable(
                        definition.getFieldCode(),
                        definition.getFieldName(),
                        candidates,
                        extractorType,
                        counter,
                        Boolean.TRUE.equals(definition.getDeduplicateByNormalized())
                ));
                continue;
            }
            fields.add(resolveSingle(
                    definition.getFieldCode(),
                    definition.getFieldName(),
                    candidates,
                    extractorType,
                    counter
            ));
        }
        return fields;
    }

    public List<ExtractedContractField> extract(List<KbChunkEntity> chunks) {
        return extract(0L, chunks);
    }

    private ExtractedContractField resolveSingle(String code,
                                                 String name,
                                                 List<Candidate> candidates,
                                                 String extractorType,
                                                 int[] counter) {
        if (candidates.isEmpty()) {
            return missingField(code, name, extractorType, counter[0]++, "未在文档中识别到该字段");
        }
        Map<String, Candidate> unique = uniqueByNormalized(candidates);
        if (unique.size() == 1) {
            Candidate candidate = unique.values().iterator().next();
            return extractedField(code, name, candidate, extractorType, counter[0]++, null);
        }
        Candidate first = unique.values().iterator().next();
        ExtractedContractField field = new ExtractedContractField();
        field.setFieldCode(code);
        field.setFieldName(name);
        field.setRawValue(String.join(" | ", unique.keySet()));
        field.setNormalizedValue(null);
        field.setStatus(ContractFieldStatus.UNCERTAIN);
        field.setConfidence(first.confidence());
        field.setEvidenceText(first.evidence());
        field.setSourceChunkRef(first.chunkRef());
        field.setExtractorType(extractorType);
        field.setFieldOrder(counter[0]++);
        field.setExplanation("识别到多个候选值，需人工确认");
        return field;
    }

    private List<ExtractedContractField> resolveRepeatable(String code,
                                                           String name,
                                                           List<Candidate> candidates,
                                                           String extractorType,
                                                           int[] counter,
                                                           boolean deduplicateByNormalized) {
        List<ExtractedContractField> fields = new ArrayList<>();
        if (candidates.isEmpty()) {
            fields.add(missingField(code, name, extractorType, counter[0]++, "未在文档中识别到该字段"));
            return fields;
        }
        Collection<Candidate> values = deduplicateByNormalized ? uniqueByNormalized(candidates).values() : limit(candidates);
        int groupIndex = 1;
        for (Candidate candidate : values) {
            ExtractedContractField field = extractedField(code, name, candidate, extractorType, counter[0]++, null);
            field.setGroupKey(code + "-" + groupIndex++);
            fields.add(field);
        }
        return fields;
    }

    private ExtractedContractField missingField(String code,
                                                String name,
                                                String extractorType,
                                                int fieldOrder,
                                                String explanation) {
        ExtractedContractField field = new ExtractedContractField();
        field.setFieldCode(code);
        field.setFieldName(name);
        field.setStatus(ContractFieldStatus.MISSING);
        field.setConfidence(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
        field.setExtractorType(extractorType);
        field.setFieldOrder(fieldOrder);
        field.setExplanation(explanation);
        return field;
    }

    private ExtractedContractField extractedField(String code,
                                                  String name,
                                                  Candidate candidate,
                                                  String extractorType,
                                                  int fieldOrder,
                                                  String explanation) {
        ExtractedContractField field = new ExtractedContractField();
        field.setFieldCode(code);
        field.setFieldName(name);
        field.setRawValue(candidate.raw());
        field.setNormalizedValue(candidate.normalized());
        field.setStatus(ContractFieldStatus.EXTRACTED);
        field.setConfidence(candidate.confidence());
        field.setEvidenceText(candidate.evidence());
        field.setSourceChunkRef(candidate.chunkRef());
        field.setExtractorType(extractorType);
        field.setFieldOrder(fieldOrder);
        field.setExplanation(explanation);
        return field;
    }

    private List<Candidate> collectCandidates(List<KbChunkEntity> chunks, ContractFieldDefinitionEntity definition) {
        String kind = definition.getExtractorKind();
        if ("PARTY_PATTERN".equals(kind)) {
            return collectParties(chunks, compilePattern(definition));
        }
        if ("AMOUNT_PATTERN".equals(kind)) {
            return collectAmounts(chunks, compilePattern(definition));
        }
        if ("DATE_KEYWORD".equals(kind)) {
            return collectKeywordDates(chunks, splitKeywords(definition.getKeywordConfig()), compilePattern(definition));
        }
        if ("KEYWORD_LINE".equals(kind)) {
            return collectKeywordLines(chunks, splitKeywords(definition.getKeywordConfig()));
        }
        throw new IllegalStateException("不支持的字段抽取器类型: " + kind);
    }

    private Pattern compilePattern(ContractFieldDefinitionEntity definition) {
        if (!StringUtils.hasText(definition.getPatternExpr())) {
            throw new IllegalStateException("字段定义缺少 patternExpr: " + definition.getFieldCode());
        }
        return Pattern.compile(definition.getPatternExpr());
    }

    private String extractorTypeLabel(String extractorKind) {
        if ("KEYWORD_LINE".equals(extractorKind)) {
            return "semantic";
        }
        return "pattern";
    }

    private List<Candidate> collectParties(List<KbChunkEntity> chunks, Pattern pattern) {
        List<Candidate> candidates = new ArrayList<>();
        for (KbChunkEntity chunk : chunks) {
            Matcher matcher = pattern.matcher(chunk.getContent());
            while (matcher.find()) {
                String raw = cleanupText(matcher.group(1));
                if (!StringUtils.hasText(raw)) {
                    continue;
                }
                candidates.add(new Candidate(raw, raw, evidence(chunk.getContent(), matcher.group(0)), chunkRef(chunk), BigDecimal.valueOf(0.9500d)));
            }
        }
        return candidates;
    }

    private List<Candidate> collectAmounts(List<KbChunkEntity> chunks, Pattern pattern) {
        List<Candidate> candidates = new ArrayList<>();
        for (KbChunkEntity chunk : chunks) {
            Matcher matcher = pattern.matcher(chunk.getContent());
            while (matcher.find()) {
                String raw = cleanupText(matcher.group(0));
                String normalized = normalizeAmount(matcher.group(1), matcher.group(2));
                if (!StringUtils.hasText(normalized)) {
                    continue;
                }
                candidates.add(new Candidate(raw, normalized, evidence(chunk.getContent(), matcher.group(0)), chunkRef(chunk), BigDecimal.valueOf(0.9100d)));
            }
        }
        return candidates;
    }

    private List<Candidate> collectKeywordDates(List<KbChunkEntity> chunks, List<String> keywords, Pattern datePattern) {
        List<Candidate> candidates = new ArrayList<>();
        for (KbChunkEntity chunk : chunks) {
            for (String line : splitLines(chunk.getContent())) {
                if (!containsAny(line, keywords)) {
                    continue;
                }
                Matcher matcher = datePattern.matcher(line);
                while (matcher.find()) {
                    String raw = matcher.group(0);
                    String normalized = normalizeDate(matcher.group(1), matcher.group(2), matcher.group(3));
                    if (!StringUtils.hasText(normalized)) {
                        continue;
                    }
                    candidates.add(new Candidate(raw, normalized, evidence(chunk.getContent(), line), chunkRef(chunk), BigDecimal.valueOf(0.9000d)));
                }
            }
        }
        return candidates;
    }

    private List<Candidate> collectKeywordLines(List<KbChunkEntity> chunks, List<String> keywords) {
        List<Candidate> candidates = new ArrayList<>();
        for (KbChunkEntity chunk : chunks) {
            for (String line : splitLines(chunk.getContent())) {
                if (!containsAny(line, keywords)) {
                    continue;
                }
                String cleaned = cleanupLine(line);
                if (!StringUtils.hasText(cleaned)) {
                    continue;
                }
                candidates.add(new Candidate(cleaned, cleaned, evidence(chunk.getContent(), cleaned), chunkRef(chunk), BigDecimal.valueOf(0.8200d)));
            }
        }
        return limit(candidates);
    }

    private boolean containsAny(String line, Collection<String> keywords) {
        if (!StringUtils.hasText(line)) {
            return false;
        }
        for (String keyword : keywords) {
            if (line.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private List<String> splitKeywords(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        String normalized = value.replace('，', '\n').replace(',', '\n').replace(';', '\n').replace('；', '\n');
        List<String> result = new ArrayList<>();
        for (String item : normalized.split("\\r?\\n")) {
            if (StringUtils.hasText(item)) {
                result.add(item.trim());
            }
        }
        return result;
    }

    private Map<String, Candidate> uniqueByNormalized(List<Candidate> candidates) {
        Map<String, Candidate> unique = new LinkedHashMap<>();
        for (Candidate candidate : candidates) {
            String key = StringUtils.hasText(candidate.normalized()) ? candidate.normalized() : candidate.raw();
            unique.putIfAbsent(key, candidate);
        }
        return unique;
    }

    private List<Candidate> limit(List<Candidate> candidates) {
        if (candidates.size() <= properties.getExtraction().getMaxRepeatableFields()) {
            return candidates;
        }
        return new ArrayList<>(candidates.subList(0, properties.getExtraction().getMaxRepeatableFields()));
    }

    private String normalizeAmount(String rawNumber, String unit) {
        if (!StringUtils.hasText(rawNumber)) {
            return null;
        }
        String normalized = rawNumber.replace("人民币", "").replace(",", "").trim();
        try {
            BigDecimal amount = new BigDecimal(normalized);
            if ("万元".equals(unit)) {
                amount = amount.multiply(BigDecimal.valueOf(10000L));
            }
            return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String normalizeDate(String year, String month, String day) {
        try {
            LocalDate date = LocalDate.of(Integer.parseInt(year), Integer.parseInt(month), Integer.parseInt(day));
            return date.format(DateTimeFormatter.ISO_DATE);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String evidence(String content, String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String normalizedContent = content == null ? "" : content;
        int index = normalizedContent.indexOf(raw);
        if (index < 0) {
            return truncate(raw);
        }
        int radius = Math.max(24, properties.getExtraction().getMaxEvidenceLength() / 2);
        int start = Math.max(0, index - radius);
        int end = Math.min(normalizedContent.length(), index + raw.length() + radius);
        return truncate(cleanupLine(normalizedContent.substring(start, end)));
    }

    private String truncate(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        String cleaned = cleanupLine(value);
        int max = Math.max(40, properties.getExtraction().getMaxEvidenceLength());
        if (cleaned.length() <= max) {
            return cleaned;
        }
        return cleaned.substring(0, max);
    }

    private String chunkRef(KbChunkEntity chunk) {
        return "chunk:" + chunk.getChunkOrder();
    }

    private List<String> splitLines(String text) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        String[] lines = text.split("\\r?\\n");
        List<String> result = new ArrayList<>(lines.length);
        for (String line : lines) {
            if (StringUtils.hasText(line)) {
                result.add(line.trim());
            }
        }
        return result;
    }

    private String cleanupText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.replaceAll("[：:]+$", "").replaceAll("\\s+", " ").trim();
    }

    private String cleanupLine(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String cleaned = value.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
        cleaned = cleaned.replaceAll("^[0-9一二三四五六七八九十、.（）()\\-]+", "").trim();
        return cleaned;
    }

    private record Candidate(String raw, String normalized, String evidence, String chunkRef, BigDecimal confidence) {
    }
}
