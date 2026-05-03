package com.legal.knowledge.service;

import com.legal.common.JsonUtils;
import com.legal.knowledge.entity.KbDocumentCleaningLogEntity;
import com.legal.knowledge.entity.KbDocumentEntity;
import com.legal.knowledge.mapper.KbDocumentCleaningLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class DocumentCleaningLogService {

    private final KbDocumentCleaningLogMapper cleaningLogMapper;

    public DocumentCleaningLogService(KbDocumentCleaningLogMapper cleaningLogMapper) {
        this.cleaningLogMapper = cleaningLogMapper;
    }

    public void save(KbDocumentEntity document, String contentFormat, DocumentCleaningReport report) {
        if (document == null || report == null) {
            return;
        }
        KbDocumentCleaningLogEntity entity = new KbDocumentCleaningLogEntity();
        entity.setTenantId(document.getTenantId());
        entity.setDocumentId(document.getDocumentId());
        entity.setDocVersion(document.getDocVersion());
        entity.setParseMethod(document.getParseMethod() == null ? null : document.getParseMethod().getCode());
        entity.setContentFormat(contentFormat);
        entity.setOriginalChars(report.getOriginalChars());
        entity.setCleanedChars(report.getCleanedChars());
        entity.setRemovedLineCount(report.getRemovedLineCount());
        entity.setRemovedChunkCount(report.getRemovedChunkCount());
        entity.setReasonSummaryJson(JsonUtils.toJson(report.getReasonSummary()));
        entity.setRemovedSamplesJson(JsonUtils.toJson(report.getRemovedSamples()));
        entity.setCreatedAt(LocalDateTime.now());
        cleaningLogMapper.insert(entity);
        log.info("document cleaning completed documentId={}, docVersion={}, originalChars={}, cleanedChars={}, removedLines={}, removedChunks={}",
                document.getDocumentId(), document.getDocVersion(), report.getOriginalChars(), report.getCleanedChars(),
                report.getRemovedLineCount(), report.getRemovedChunkCount());
    }
}
