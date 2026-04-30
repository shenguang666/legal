package com.legal.memory.service;

import com.legal.common.JsonUtils;
import com.legal.config.LegalMemoryProperties;
import com.legal.enums.MemoryTaskType;
import com.legal.memory.entity.MemoryTaskOutboxEntity;
import com.legal.memory.mapper.MemoryTaskOutboxMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 记忆任务 outbox worker：处理摘要刷新、长期记忆自动抽取、用户外挂知识索引。
 */
@Slf4j
@Component
public class MemoryTaskOutboxWorker {

    private final LegalMemoryProperties props;
    private final MemoryTaskOutboxMapper outboxMapper;
    private final MemorySummaryService summaryService;
    private final LongTermMemoryExtractionService extractionService;
    private final UserKnowledgeService userKnowledgeService;

    public MemoryTaskOutboxWorker(LegalMemoryProperties props,
                                  MemoryTaskOutboxMapper outboxMapper,
                                  MemorySummaryService summaryService,
                                  LongTermMemoryExtractionService extractionService,
                                  UserKnowledgeService userKnowledgeService) {
        this.props = props;
        this.outboxMapper = outboxMapper;
        this.summaryService = summaryService;
        this.extractionService = extractionService;
        this.userKnowledgeService = userKnowledgeService;
    }

    @Scheduled(fixedDelayString = "${legal.memory.worker.poll-interval-ms:3000}")
    public void poll() {
        if (!props.isEnabled() || !props.getWorker().isEnabled()) return;
        List<MemoryTaskOutboxEntity> tasks = outboxMapper.selectReadyTasks(props.getWorker().getBatchSize(), props.getWorker().getMaxRetries());
        for (MemoryTaskOutboxEntity task : tasks) processTask(task);
    }

    private void processTask(MemoryTaskOutboxEntity task) {
        if (outboxMapper.markProcessing(task.getId()) == 0) return;
        try {
            if (task.getTaskType() == MemoryTaskType.SUMMARY_REFRESH) {
                summaryService.refreshSummary(task.getTenantId(), task.getUserId(), task.getSessionId());
                String summaryText = summaryService.findSummaryText(task.getTenantId(), task.getUserId(), task.getSessionId());
                extractionService.extractFromSummary(task.getTenantId(), task.getUserId(), task.getSessionId(), summaryText);
                userKnowledgeService.reviewPendingBySummary(task.getTenantId(), task.getUserId(), task.getSessionId(), summaryText);
            } else if (task.getTaskType() == MemoryTaskType.USER_KNOWLEDGE_INDEX) {
                MemoryTaskPayloads.QaPayload payload = JsonUtils.fromJson(task.getPayload(), MemoryTaskPayloads.QaPayload.class);
                userKnowledgeService.classifyAndStore(task.getTenantId(), task.getUserId(), task.getSessionId(), payload);
            } else {
                log.info("skip deprecated/unsupported memory task type={}, id={}", task.getTaskType(), task.getId());
            }
            outboxMapper.markDone(task.getId());
        } catch (RuntimeException ex) {
            log.error("处理记忆任务失败, outboxId={}, type={}", task.getId(), task.getTaskType(), ex);
            outboxMapper.markFailed(task.getId(), Math.max(1, props.getWorker().getRetryDelaySeconds()), ex.getMessage());
        }
    }
}
