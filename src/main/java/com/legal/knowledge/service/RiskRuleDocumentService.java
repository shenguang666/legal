package com.legal.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.legal.common.AppException;
import com.legal.enums.DocumentParseMethod;
import com.legal.enums.DocumentParseStatus;
import com.legal.enums.KbDocumentBizType;
import com.legal.enums.KbDocumentStatus;
import com.legal.enums.KbIndexStatus;
import com.legal.enums.KbOutboxOp;
import com.legal.enums.KbOutboxStatus;
import com.legal.knowledge.dto.DocumentDto;
import com.legal.knowledge.entity.KbChunkEntity;
import com.legal.knowledge.entity.KbDocumentEntity;
import com.legal.knowledge.entity.KbIndexOutboxEntity;
import com.legal.knowledge.mapper.KbChunkMapper;
import com.legal.knowledge.mapper.KbDocumentMapper;
import com.legal.knowledge.mapper.KbIndexOutboxMapper;
import com.legal.retrieval.service.ElasticsearchChunkStore;
import com.legal.security.AuthPrincipal;
import com.legal.security.IdempotencyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RiskRuleDocumentService {

    private final KbDocumentMapper kbDocumentMapper;
    private final KbChunkMapper kbChunkMapper;
    private final KbIndexOutboxMapper kbIndexOutboxMapper;
    private final IdempotencyService idempotencyService;
    private final DocumentTextExtractor documentTextExtractor;
    private final DocumentChunker documentChunker;
    private final ElasticsearchChunkStore elasticsearchChunkStore;
    private final DocumentImportService documentImportService;
    private final DocumentParseTaskService documentParseTaskService;

    public RiskRuleDocumentService(KbDocumentMapper kbDocumentMapper,
                                   KbChunkMapper kbChunkMapper,
                                   KbIndexOutboxMapper kbIndexOutboxMapper,
                                   IdempotencyService idempotencyService,
                                   DocumentTextExtractor documentTextExtractor,
                                   DocumentChunker documentChunker,
                                   ElasticsearchChunkStore elasticsearchChunkStore,
                                   DocumentImportService documentImportService,
                                   DocumentParseTaskService documentParseTaskService) {
        this.kbDocumentMapper = kbDocumentMapper;
        this.kbChunkMapper = kbChunkMapper;
        this.kbIndexOutboxMapper = kbIndexOutboxMapper;
        this.idempotencyService = idempotencyService;
        this.documentTextExtractor = documentTextExtractor;
        this.documentChunker = documentChunker;
        this.elasticsearchChunkStore = elasticsearchChunkStore;
        this.documentImportService = documentImportService;
        this.documentParseTaskService = documentParseTaskService;
    }

    @Transactional
    public DocumentDto importDocument(AuthPrincipal principal,
                                      String requestId,
                                      MultipartFile file,
                                      String title,
                                      String source,
                                      Integer chunkSize,
                                      Integer chunkOverlap) {
        return importDocument(principal, requestId, file, title, source, chunkSize, chunkOverlap, null, false);
    }

    @Transactional
    public DocumentDto importDocument(AuthPrincipal principal,
                                      String requestId,
                                      MultipartFile file,
                                      String title,
                                      String source,
                                      Integer chunkSize,
                                      Integer chunkOverlap,
                                      String parseMethod,
                                      Boolean cleaningEnabled) {
        ensureElasticsearchEnabled();
        idempotencyService.ensureUnique(principal, "risk-rule:import-document", requestId);
        KbDocumentEntity document = documentImportService.importDocument(
                principal,
                file,
                title,
                source,
                KbDocumentBizType.RISK_RULE,
                parseMethod,
                chunkSize,
                chunkOverlap,
                cleaningEnabled,
                "文档内容过短，无法生成风险规则切片"
        );
        return toDto(document);
    }

    @Transactional
    public DocumentDto createManualDocument(AuthPrincipal principal,
                                            String requestId,
                                            String content,
                                            String title,
                                            String source,
                                            Integer chunkSize,
                                            Integer chunkOverlap) {
        ensureElasticsearchEnabled();
        idempotencyService.ensureUnique(principal, "risk-rule:create-manual-document", requestId);
        if (!StringUtils.hasText(content)) {
            throw AppException.badRequest("手工录入的风险规则内容不能为空");
        }
        List<String> chunks = buildChunks(content.trim(), chunkSize, chunkOverlap);
        if (chunks.isEmpty()) {
            throw AppException.badRequest("风险规则内容过短，无法生成切片");
        }

        String documentTitle = StringUtils.hasText(title) ? title.trim() : "手工风险规则";
        String documentSource = StringUtils.hasText(source) ? source.trim() : "手工录入";
        LocalDateTime now = LocalDateTime.now();

        KbDocumentEntity document = new KbDocumentEntity();
        document.setTenantId(principal.tenantId());
        document.setOwnerUserId(principal.userId());
        document.setTitle(documentTitle);
        document.setSource(documentSource);
        document.setBizType(KbDocumentBizType.RISK_RULE);
        document.setStatus(KbDocumentStatus.PROCESSING);
        document.setDocVersion(1);
        document.setIndexStatus(KbIndexStatus.PENDING);
        document.setParseMethod(DocumentParseMethod.NATIVE);
        document.setParseStatus(DocumentParseStatus.COMPLETED);
        document.setParseCompletedAt(now);
        document.setCreatedAt(now);
        document.setUpdatedAt(now);
        kbDocumentMapper.insert(document);

        persistChunks(principal.tenantId(), document.getDocumentId(), 1, chunks, now);
        enqueueOutbox(principal.tenantId(), document.getDocumentId(), 1, KbOutboxOp.UPSERT, now);
        return toDto(document);
    }

    public List<DocumentDto> listDocuments(AuthPrincipal principal) {
        return kbDocumentMapper.selectList(
                        new LambdaQueryWrapper<KbDocumentEntity>()
                                .eq(KbDocumentEntity::getTenantId, principal.tenantId())
                                .eq(KbDocumentEntity::getOwnerUserId, principal.userId())
                                .eq(KbDocumentEntity::getBizType, KbDocumentBizType.RISK_RULE)
                                .ne(KbDocumentEntity::getStatus, KbDocumentStatus.DELETED)
                                .orderByDesc(KbDocumentEntity::getUpdatedAt)
                ).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public DocumentDto triggerIndex(AuthPrincipal principal, Long documentId, String requestId) {
        ensureElasticsearchEnabled();
        idempotencyService.ensureUnique(principal, "risk-rule:trigger-index", requestId);
        KbDocumentEntity document = requireDocument(principal, documentId);
        if (document.getStatus() == KbDocumentStatus.DELETED) {
            throw AppException.badRequest("已删除文档无法触发索引");
        }

        int fromVersion = document.getDocVersion();
        int nextVersion = fromVersion + 1;
        int copied = kbChunkMapper.copyVersion(principal.tenantId(), documentId, fromVersion, nextVersion);
        if (copied <= 0) {
            throw AppException.badRequest("文档暂无可索引内容，请先导入文件");
        }

        document.setDocVersion(nextVersion);
        document.setStatus(KbDocumentStatus.PROCESSING);
        document.setIndexStatus(KbIndexStatus.PENDING);
        document.setUpdatedAt(LocalDateTime.now());
        kbDocumentMapper.updateById(document);

        enqueueOutbox(principal.tenantId(), documentId, nextVersion, KbOutboxOp.UPSERT, LocalDateTime.now());
        return toDto(document);
    }

    @Transactional
    public DocumentDto deleteDocument(AuthPrincipal principal, Long documentId, String requestId) {
        ensureElasticsearchEnabled();
        idempotencyService.ensureUnique(principal, "risk-rule:delete-document", requestId);
        KbDocumentEntity document = requireDocument(principal, documentId);
        if (document.getStatus() == KbDocumentStatus.DELETED) {
            return toDto(document);
        }

        int nextVersion = document.getDocVersion() + 1;
        document.setDocVersion(nextVersion);
        document.setStatus(KbDocumentStatus.DELETED);
        document.setIndexStatus(KbIndexStatus.PENDING);
        document.setUpdatedAt(LocalDateTime.now());
        kbDocumentMapper.updateById(document);

        enqueueOutbox(principal.tenantId(), documentId, nextVersion, KbOutboxOp.DELETE, LocalDateTime.now());
        return toDto(document);
    }

    @Transactional
    public DocumentDto retryParsing(AuthPrincipal principal, Long documentId, String requestId) {
        idempotencyService.ensureUnique(principal, "risk-rule:retry-parsing", requestId);
        documentParseTaskService.retryFailed(principal.tenantId(), principal.userId(), documentId);
        return toDto(requireDocument(principal, documentId));
    }

    private KbDocumentEntity requireDocument(AuthPrincipal principal, Long documentId) {
        KbDocumentEntity document = kbDocumentMapper.selectOne(
                new LambdaQueryWrapper<KbDocumentEntity>()
                        .eq(KbDocumentEntity::getDocumentId, documentId)
                        .eq(KbDocumentEntity::getTenantId, principal.tenantId())
                        .eq(KbDocumentEntity::getOwnerUserId, principal.userId())
                        .eq(KbDocumentEntity::getBizType, KbDocumentBizType.RISK_RULE)
                        .last("limit 1")
        );
        if (document == null) {
            throw AppException.notFound("风险规则文档不存在");
        }
        return document;
    }

    private void ensureElasticsearchEnabled() {
        if (!elasticsearchChunkStore.isEnabled()) {
            throw AppException.badRequest("当前环境未启用 Elasticsearch，无法执行风险规则索引相关操作");
        }
    }

    private List<String> buildChunks(String extractedText, Integer chunkSize, Integer chunkOverlap) {
        if (chunkSize == null) {
            return documentChunker.chunk(extractedText);
        }
        if (chunkOverlap == null) {
            int overlap = (int) Math.round(chunkSize * 0.15d);
            return documentChunker.chunk(extractedText, chunkSize, overlap);
        }
        return documentChunker.chunk(extractedText, chunkSize, chunkOverlap);
    }

    private void persistChunks(Long tenantId,
                               Long documentId,
                               int docVersion,
                               List<String> chunks,
                               LocalDateTime now) {
        for (int i = 0; i < chunks.size(); i++) {
            String content = chunks.get(i);
            KbChunkEntity entity = new KbChunkEntity();
            entity.setTenantId(tenantId);
            entity.setDocumentId(documentId);
            entity.setDocVersion(docVersion);
            entity.setChunkOrder(i + 1);
            entity.setContent(content);
            entity.setContentHash(sha256(content));
            entity.setCreatedAt(now);
            kbChunkMapper.insert(entity);
        }
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

    private String fallbackTitle(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return "未命名风险规则文档";
        }
        int dot = fileName.lastIndexOf('.');
        if (dot <= 0) {
            return fileName;
        }
        return fileName.substring(0, dot);
    }

    private String fallbackSource(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return "上传文件";
        }
        return fileName;
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

    private DocumentDto toDto(KbDocumentEntity entity) {
        DocumentDto dto = new DocumentDto();
        dto.setDocumentId(entity.getDocumentId());
        dto.setTitle(entity.getTitle());
        dto.setSource(entity.getSource());
        dto.setBizType(entity.getBizType() == null ? null : entity.getBizType().getCode());
        dto.setStatus(entity.getStatus() == null ? null : entity.getStatus().getCode());
        dto.setIndexStatus(entity.getIndexStatus() == null ? null : entity.getIndexStatus().getCode());
        dto.setParseMethod(entity.getParseMethod() == null ? null : entity.getParseMethod().getCode());
        dto.setParseStatus(entity.getParseStatus() == null ? null : entity.getParseStatus().getCode());
        dto.setParseFailureReason(entity.getParseFailureReason());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
