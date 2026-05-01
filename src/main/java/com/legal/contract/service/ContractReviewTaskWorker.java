package com.legal.contract.service;

import com.legal.config.ContractReviewProperties;
import com.legal.contract.entity.ContractReviewTaskEntity;
import com.legal.contract.mapper.ContractReviewTaskMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class ContractReviewTaskWorker {

    private final ContractReviewProperties properties;
    private final ContractReviewTaskMapper contractReviewTaskMapper;
    private final ContractReviewProcessingService contractReviewProcessingService;

    public ContractReviewTaskWorker(ContractReviewProperties properties,
                                    ContractReviewTaskMapper contractReviewTaskMapper,
                                    ContractReviewProcessingService contractReviewProcessingService) {
        this.properties = properties;
        this.contractReviewTaskMapper = contractReviewTaskMapper;
        this.contractReviewProcessingService = contractReviewProcessingService;
    }

    @Scheduled(fixedDelayString = "${legal.contract-review.worker.poll-interval-ms:3000}")
    public void poll() {
        if (!properties.isEnabled() || !properties.getWorker().isEnabled()) {
            return;
        }
        List<ContractReviewTaskEntity> tasks = contractReviewTaskMapper.selectReadyTasks(
                properties.getWorker().getBatchSize(),
                properties.getWorker().getMaxRetries()
        );
        for (ContractReviewTaskEntity task : tasks) {
            processTask(task);
        }
    }

    private void processTask(ContractReviewTaskEntity task) {
        if (contractReviewTaskMapper.markProcessing(task.getId()) == 0) {
            return;
        }
        try {
            contractReviewProcessingService.processTask(task);
            contractReviewTaskMapper.markDone(task.getId());
        } catch (RuntimeException ex) {
            log.error("处理合同审阅任务失败, taskId={}, reviewId={}, docId={}", task.getId(), task.getReviewId(), task.getDocumentId(), ex);
            long retryDelaySeconds = Math.max(1, properties.getWorker().getRetryDelaySeconds());
            contractReviewTaskMapper.markFailed(task.getId(), retryDelaySeconds, ex.getMessage());
            contractReviewProcessingService.markReviewFailed(task, ex.getMessage());
        }
    }
}
