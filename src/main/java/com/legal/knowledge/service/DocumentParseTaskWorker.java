package com.legal.knowledge.service;

import com.legal.config.DocumentProcessingProperties;
import com.legal.knowledge.entity.KbDocumentParseTaskEntity;
import com.legal.knowledge.mapper.KbDocumentParseTaskMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class DocumentParseTaskWorker {

    private final DocumentProcessingProperties properties;
    private final KbDocumentParseTaskMapper parseTaskMapper;
    private final DocumentParseTaskService parseTaskService;

    public DocumentParseTaskWorker(DocumentProcessingProperties properties,
                                   KbDocumentParseTaskMapper parseTaskMapper,
                                   DocumentParseTaskService parseTaskService) {
        this.properties = properties;
        this.parseTaskMapper = parseTaskMapper;
        this.parseTaskService = parseTaskService;
    }

    @Scheduled(fixedDelayString = "${legal.document-processing.worker.poll-interval-ms:3000}")
    public void poll() {
        if (!properties.getWorker().isEnabled()) {
            return;
        }
        List<KbDocumentParseTaskEntity> tasks = parseTaskMapper.selectReadyTasks(
                properties.getWorker().getBatchSize(),
                properties.getWorker().getMaxRetries()
        );
        for (KbDocumentParseTaskEntity task : tasks) {
            processTask(task);
        }
    }

    private void processTask(KbDocumentParseTaskEntity task) {
        if (parseTaskMapper.markProcessing(task.getTaskId()) == 0) {
            return;
        }
        try {
            parseTaskService.processTask(task);
        } catch (RuntimeException ex) {
            log.error("处理文档解析任务失败, taskId={}, documentId={}, parseMethod={}",
                    task.getTaskId(), task.getDocumentId(), task.getParseMethod(), ex);
        }
    }
}
