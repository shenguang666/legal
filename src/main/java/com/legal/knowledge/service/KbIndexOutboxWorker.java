package com.legal.knowledge.service;

import com.legal.config.ElasticsearchProperties;
import com.legal.knowledge.entity.KbIndexOutboxEntity;
import com.legal.knowledge.mapper.KbIndexOutboxMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class KbIndexOutboxWorker {

    private final ElasticsearchProperties elasticsearchProperties;
    private final KbIndexOutboxMapper kbIndexOutboxMapper;
    private final KnowledgeIndexService knowledgeIndexService;

    public KbIndexOutboxWorker(ElasticsearchProperties elasticsearchProperties,
                               KbIndexOutboxMapper kbIndexOutboxMapper,
                               KnowledgeIndexService knowledgeIndexService) {
        this.elasticsearchProperties = elasticsearchProperties;
        this.kbIndexOutboxMapper = kbIndexOutboxMapper;
        this.knowledgeIndexService = knowledgeIndexService;
    }

    @Scheduled(fixedDelayString = "${elasticsearch.worker.poll-interval-ms:3000}")
    public void poll() {
        if (!elasticsearchProperties.isEnabled() || !elasticsearchProperties.getWorker().isEnabled()) {
            return;
        }

        List<KbIndexOutboxEntity> tasks = kbIndexOutboxMapper.selectReadyTasks(
                elasticsearchProperties.getWorker().getBatchSize(),
                elasticsearchProperties.getWorker().getMaxRetries()
        );
        for (KbIndexOutboxEntity task : tasks) {
            processTask(task);
        }
    }

    private void processTask(KbIndexOutboxEntity task) {
        if (kbIndexOutboxMapper.markProcessing(task.getId()) == 0) {
            return;
        }
        try {
            knowledgeIndexService.processTask(task);
            kbIndexOutboxMapper.markDone(task.getId());
        } catch (RuntimeException ex) {
            log.error("处理索引任务失败, outboxId={}, docId={}, op={}",
                    task.getId(), task.getDocumentId(), task.getOp(), ex);
            long retryDelaySeconds = Math.max(1, elasticsearchProperties.getWorker().getRetryDelay().toSeconds());
            kbIndexOutboxMapper.markFailed(task.getId(), retryDelaySeconds);
            knowledgeIndexService.markDocumentFailed(task);
        }
    }
}
