package com.legal.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.legal.common.AppException;
import com.legal.config.DocumentProcessingProperties;
import com.legal.enums.DocumentParseMethod;
import com.legal.enums.DocumentParseStatus;
import com.legal.enums.KbDocumentBizType;
import com.legal.enums.KbDocumentStatus;
import com.legal.enums.KbIndexStatus;
import com.legal.enums.KbOutboxOp;
import com.legal.enums.KbOutboxStatus;
import com.legal.knowledge.entity.KbChunkEntity;
import com.legal.knowledge.entity.KbDocumentEntity;
import com.legal.knowledge.entity.KbDocumentParseTaskEntity;
import com.legal.knowledge.entity.KbIndexOutboxEntity;
import com.legal.knowledge.mapper.KbChunkMapper;
import com.legal.knowledge.mapper.KbDocumentMapper;
import com.legal.knowledge.mapper.KbDocumentParseTaskMapper;
import com.legal.knowledge.mapper.KbIndexOutboxMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DocumentParseTaskService {

    private final KbDocumentMapper kbDocumentMapper;
    private final KbChunkMapper kbChunkMapper;
    private final KbIndexOutboxMapper kbIndexOutboxMapper;
    private final KbDocumentParseTaskMapper parseTaskMapper;
    private final NativeDocumentParser nativeDocumentParser;
    private final MineruClient mineruClient;
    private final SemanticDocumentChunker semanticDocumentChunker;
    private final DocumentContentCleaner documentContentCleaner;
    private final DocumentCleaningLogService cleaningLogService;
    private final DocumentProcessingProperties properties;

    public DocumentParseTaskService(KbDocumentMapper kbDocumentMapper,
                                    KbChunkMapper kbChunkMapper,
                                    KbIndexOutboxMapper kbIndexOutboxMapper,
                                    KbDocumentParseTaskMapper parseTaskMapper,
                                    NativeDocumentParser nativeDocumentParser,
                                    MineruClient mineruClient,
                                    SemanticDocumentChunker semanticDocumentChunker,
                                    DocumentContentCleaner documentContentCleaner,
                                    DocumentCleaningLogService cleaningLogService,
                                    DocumentProcessingProperties properties) {
        this.kbDocumentMapper = kbDocumentMapper;
        this.kbChunkMapper = kbChunkMapper;
        this.kbIndexOutboxMapper = kbIndexOutboxMapper;
        this.parseTaskMapper = parseTaskMapper;
        this.nativeDocumentParser = nativeDocumentParser;
        this.mineruClient = mineruClient;
        this.semanticDocumentChunker = semanticDocumentChunker;
        this.documentContentCleaner = documentContentCleaner;
        this.cleaningLogService = cleaningLogService;
        this.properties = properties;
    }

    @Transactional
    public KbDocumentParseTaskEntity createTask(KbDocumentEntity document, String fileName, byte[] fileContent) {
        LocalDateTime now = LocalDateTime.now();
        KbDocumentParseTaskEntity task = new KbDocumentParseTaskEntity();
        task.setTenantId(document.getTenantId());
        task.setDocumentId(document.getDocumentId());
        task.setDocVersion(document.getDocVersion());
        task.setParseMethod(document.getParseMethod());
        task.setParseStatus(DocumentParseStatus.PENDING);
        task.setCleaningEnabled(Boolean.TRUE.equals(document.getCleaningEnabled()));
        task.setFileName(StringUtils.hasText(fileName) ? fileName : "未命名文档");
        task.setFileContent(fileContent);
        task.setRetryCount(0);
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        parseTaskMapper.insert(task);
        return task;
    }

    @Transactional
    public void completeNative(KbDocumentEntity document, List<String> chunks, DocumentCleaningReport cleaningReport) {
        completeDocument(document, chunks, cleaningReport, "TEXT", null, null, null);
    }

    public List<String> buildNativeChunks(String text, Integer chunkSize, Integer chunkOverlap) {
        return nativeDocumentParser.buildChunks(text, chunkSize, chunkOverlap);
    }

    @Transactional
    public void processTask(KbDocumentParseTaskEntity task) {
        if (task.getParseMethod() == DocumentParseMethod.NATIVE) {
            markTaskFailed(task, "原生解析任务不应进入异步队列");
            return;
        }
        KbDocumentEntity document = findDocument(task.getTenantId(), task.getDocumentId());
        if (document == null || document.getStatus() == KbDocumentStatus.DELETED) {
            parseTaskMapper.markFailed(task.getTaskId(), "文档不存在或已删除", retryDelaySeconds());
            return;
        }
        try {
            MineruClient.MineruUploadSession session = mineruClient.submit(task.getFileName(), task.getFileContent());
            updateMineruSession(task, document, session);
            MineruClient.MineruExtractResult result = mineruClient.waitForResult(session.batchId(), session.dataId());
            if (result.failed()) {
                throw AppException.badRequest(safeError(result.errorMessage()));
            }
            String markdown = mineruClient.downloadMarkdown(result.fullZipUrl());
            boolean cleaningEnabled = Boolean.TRUE.equals(task.getCleaningEnabled()) && properties.getCleaning().isEnabled();
            DocumentCleaningResult cleaningResult = cleaningEnabled ? documentContentCleaner.cleanMarkdown(markdown) : null;
            String chunkSource = cleaningResult == null ? markdown : cleaningResult.getContent();
            List<String> chunks = semanticDocumentChunker.chunkMarkdown(
                    chunkSource,
                    properties.getMineru().getChunkSize(),
                    properties.getMineru().getMinChunkSize()
            );
            if (chunks.isEmpty()) {
                throw AppException.badRequest("MinerU 解析结果过短，无法生成切片");
            }
            completeDocument(document, chunks, cleaningResult == null ? null : cleaningResult.getReport(), "MARKDOWN",
                    session.batchId(), session.dataId(), result.fullZipUrl());
            parseTaskMapper.markCompleted(task.getTaskId());
            clearTaskFileContent(task.getTaskId());
        } catch (RuntimeException ex) {
            markTaskFailed(task, safeError(ex.getMessage()));
        }
    }

    @Transactional
    public void retryFailed(Long tenantId, Long ownerUserId, Long documentId) {
        KbDocumentEntity document = kbDocumentMapper.selectOne(new LambdaQueryWrapper<KbDocumentEntity>()
                .eq(KbDocumentEntity::getTenantId, tenantId)
                .eq(KbDocumentEntity::getOwnerUserId, ownerUserId)
                .eq(KbDocumentEntity::getDocumentId, documentId)
                .last("limit 1"));
        if (document == null || document.getStatus() == KbDocumentStatus.DELETED) {
            throw AppException.notFound("文档不存在");
        }
        KbDocumentParseTaskEntity task = parseTaskMapper.selectOne(new LambdaQueryWrapper<KbDocumentParseTaskEntity>()
                .eq(KbDocumentParseTaskEntity::getTenantId, tenantId)
                .eq(KbDocumentParseTaskEntity::getDocumentId, documentId)
                .eq(KbDocumentParseTaskEntity::getDocVersion, document.getDocVersion())
                .orderByDesc(KbDocumentParseTaskEntity::getTaskId)
                .last("limit 1"));
        if (task == null || task.getParseStatus() != DocumentParseStatus.FAILED) {
            throw AppException.badRequest("当前文档没有可重试的解析失败任务");
        }
        if (task.getFileContent() == null) {
            throw AppException.badRequest("解析文件内容已清理，请重新上传文档");
        }
        parseTaskMapper.resetForManualRetry(task.getTaskId());
        kbDocumentMapper.resetParseFailure(tenantId, documentId, DocumentParseStatus.PENDING, KbDocumentStatus.PENDING);
    }

    private void updateMineruSession(KbDocumentParseTaskEntity task,
                                     KbDocumentEntity document,
                                     MineruClient.MineruUploadSession session) {
        task.setMineruBatchId(session.batchId());
        task.setMineruDataId(session.dataId());
        task.setUpdatedAt(LocalDateTime.now());
        parseTaskMapper.updateById(task);
        document.setMineruBatchId(session.batchId());
        document.setMineruDataId(session.dataId());
        document.setParseStatus(DocumentParseStatus.PROCESSING);
        document.setParseStartedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());
        kbDocumentMapper.updateById(document);
    }

    private void completeDocument(KbDocumentEntity document,
                                  List<String> chunks,
                                  DocumentCleaningReport cleaningReport,
                                  String contentFormat,
                                  String mineruBatchId,
                                  String mineruDataId,
                                  String mineruFullZipUrl) {
        LocalDateTime now = LocalDateTime.now();
        kbChunkMapper.deleteByDocVersion(document.getTenantId(), document.getDocumentId(), document.getDocVersion());
        DocumentCleaningReport chunkFilterReport = persistChunks(
                document.getTenantId(),
                document.getDocumentId(),
                document.getDocVersion(),
                chunks,
                now,
                Boolean.TRUE.equals(document.getCleaningEnabled())
        );
        if (cleaningReport != null) {
            cleaningReport.merge(chunkFilterReport, properties.getCleaning().getRemovedSampleLimit(), properties.getCleaning().getRemovedSampleMaxChars());
            cleaningLogService.save(document, contentFormat, cleaningReport);
        } else if (chunkFilterReport.hasRemovedContent()) {
            cleaningLogService.save(document, contentFormat == null ? "TEXT" : contentFormat, chunkFilterReport);
        }
        document.setParseStatus(DocumentParseStatus.COMPLETED);
        document.setParseFailureReason(null);
        document.setMineruBatchId(mineruBatchId);
        document.setMineruDataId(mineruDataId);
        document.setMineruFullZipUrl(mineruFullZipUrl);
        document.setParseCompletedAt(now);
        if (document.getBizType() == KbDocumentBizType.TIANYAN_REVIEW) {
            document.setStatus(KbDocumentStatus.ACTIVE);
            document.setIndexStatus(KbIndexStatus.COMPLETED);
        } else {
            document.setStatus(KbDocumentStatus.PROCESSING);
            document.setIndexStatus(KbIndexStatus.PENDING);
            enqueueOutbox(document.getTenantId(), document.getDocumentId(), document.getDocVersion(), KbOutboxOp.UPSERT, now);
        }
        document.setUpdatedAt(now);
        kbDocumentMapper.updateById(document);
    }

    private void markTaskFailed(KbDocumentParseTaskEntity task, String message) {
        parseTaskMapper.markFailed(task.getTaskId(), message, retryDelaySeconds());
        KbDocumentEntity document = findDocument(task.getTenantId(), task.getDocumentId());
        if (document == null) {
            return;
        }
        document.setParseStatus(DocumentParseStatus.FAILED);
        document.setParseFailureReason(message);
        document.setStatus(KbDocumentStatus.PENDING);
        document.setUpdatedAt(LocalDateTime.now());
        kbDocumentMapper.updateById(document);
    }

    private void clearTaskFileContent(Long taskId) {
        KbDocumentParseTaskEntity update = new KbDocumentParseTaskEntity();
        update.setTaskId(taskId);
        update.setFileContent(null);
        update.setUpdatedAt(LocalDateTime.now());
        parseTaskMapper.updateById(update);
    }

    private KbDocumentEntity findDocument(Long tenantId, Long documentId) {
        return kbDocumentMapper.selectOne(new LambdaQueryWrapper<KbDocumentEntity>()
                .eq(KbDocumentEntity::getTenantId, tenantId)
                .eq(KbDocumentEntity::getDocumentId, documentId)
                .last("limit 1"));
    }

    private void persistChunks(Long tenantId,
                               Long documentId,
                               int docVersion,
                               List<String> chunks,
                               LocalDateTime now) {
        persistChunks(tenantId, documentId, docVersion, chunks, now, false);
    }

    private DocumentCleaningReport persistChunks(Long tenantId,
                                                 Long documentId,
                                                 int docVersion,
                                                 List<String> chunks,
                                                 LocalDateTime now,
                                                 boolean filterLowQuality) {
        DocumentCleaningReport report = new DocumentCleaningReport(chunks.stream().mapToInt(value -> value == null ? 0 : value.length()).sum());
        int order = 1;
        for (String content : chunks) {
            if (filterLowQuality && documentContentCleaner.isLowQualityChunk(content)) {
                report.addRemovedChunk("LOW_QUALITY_CHUNK", content, properties.getCleaning().getRemovedSampleLimit(), properties.getCleaning().getRemovedSampleMaxChars());
                continue;
            }
            KbChunkEntity entity = new KbChunkEntity();
            entity.setTenantId(tenantId);
            entity.setDocumentId(documentId);
            entity.setDocVersion(docVersion);
            entity.setChunkOrder(order++);
            entity.setContent(content);
            entity.setContentHash(sha256(content));
            entity.setCreatedAt(now);
            kbChunkMapper.insert(entity);
        }
        report.setCleanedChars(chunks.stream()
                .filter(content -> !filterLowQuality || !documentContentCleaner.isLowQualityChunk(content))
                .mapToInt(value -> value == null ? 0 : value.length())
                .sum());
        return report;
    }

    private void enqueueOutbox(Long tenantId,
                               Long documentId,
                               int docVersion,
                               KbOutboxOp op,
                               LocalDateTime now) {
        KbIndexOutboxEntity outbox = new KbIndexOutboxEntity();
        outbox.setTenantId(tenantId);
        outbox.setDocumentId(documentId);
        outbox.setDocVersion(docVersion);
        outbox.setOp(op);
        outbox.setStatus(KbOutboxStatus.PENDING);
        outbox.setRetryCount(0);
        outbox.setNextRetryAt(null);
        outbox.setCreatedAt(now);
        outbox.setUpdatedAt(now);
        kbIndexOutboxMapper.insert(outbox);
    }

    private long retryDelaySeconds() {
        return Math.max(5L, properties.getMineru().getPollInterval().toSeconds());
    }

    private String safeError(String message) {
        String value = StringUtils.hasText(message) ? message.trim() : "文档解析失败";
        if (value.length() > 1000) {
            return value.substring(0, 1000);
        }
        return value;
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                out.append(String.format("%02x", b));
            }
            return out.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 算法不可用", ex);
        }
    }
}
