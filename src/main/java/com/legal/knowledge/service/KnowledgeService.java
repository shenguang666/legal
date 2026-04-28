package com.legal.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.legal.common.AppException;
import com.legal.knowledge.dto.CreateDocumentRequest;
import com.legal.knowledge.dto.ChunkDto;
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
import java.util.Map;

import static java.util.stream.Collectors.toList;

@Service
public class KnowledgeService {

    private final KbDocumentMapper kbDocumentMapper;
    private final KbChunkMapper kbChunkMapper;
    private final KbIndexOutboxMapper kbIndexOutboxMapper;
    private final IdempotencyService idempotencyService;
    private final DocumentTextExtractor documentTextExtractor;
    private final DocumentChunker documentChunker;
    private final ElasticsearchChunkStore elasticsearchChunkStore;

    public KnowledgeService(KbDocumentMapper kbDocumentMapper,
                            KbChunkMapper kbChunkMapper,
                            KbIndexOutboxMapper kbIndexOutboxMapper,
                            IdempotencyService idempotencyService,
                            DocumentTextExtractor documentTextExtractor,
                            DocumentChunker documentChunker,
                            ElasticsearchChunkStore elasticsearchChunkStore) {
        this.kbDocumentMapper = kbDocumentMapper;
        this.kbChunkMapper = kbChunkMapper;
        this.kbIndexOutboxMapper = kbIndexOutboxMapper;
        this.idempotencyService = idempotencyService;
        this.documentTextExtractor = documentTextExtractor;
        this.documentChunker = documentChunker;
        this.elasticsearchChunkStore = elasticsearchChunkStore;
    }

    @Transactional
    public DocumentDto createDocument(AuthPrincipal principal, CreateDocumentRequest request) {
        idempotencyService.ensureUnique(principal, "knowledge:create-document", request.getRequestId());
        KbDocumentEntity entity = new KbDocumentEntity();
        entity.setTenantId(principal.tenantId());
        entity.setOwnerUserId(principal.userId());
        entity.setTitle(request.getTitle());
        entity.setSource(request.getSource());
        entity.setStatus("PENDING");
        entity.setDocVersion(1);
        entity.setIndexStatus("PENDING");
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        kbDocumentMapper.insert(entity);
        return toDto(entity);
    }

    @Transactional
    public DocumentDto importDocument(AuthPrincipal principal,
                                      String requestId,
                                      MultipartFile file,
                                      String title,
                                      String source,
                                      Integer chunkSize,
                                      Integer chunkOverlap) {
        ensureElasticsearchEnabled();
        idempotencyService.ensureUnique(principal, "knowledge:import-document", requestId);
        String extractedText = documentTextExtractor.extract(file);
        List<String> chunks = buildChunks(extractedText, chunkSize, chunkOverlap);
        if (chunks.isEmpty()) {
            throw AppException.badRequest("文档内容过短，无法生成可检索切片");
        }

        String originalName = file.getOriginalFilename();
        String documentTitle = StringUtils.hasText(title) ? title.trim() : fallbackTitle(originalName);
        String documentSource = StringUtils.hasText(source) ? source.trim() : fallbackSource(originalName);
        LocalDateTime now = LocalDateTime.now();

        KbDocumentEntity document = new KbDocumentEntity();
        document.setTenantId(principal.tenantId());
        document.setOwnerUserId(principal.userId());
        document.setTitle(documentTitle);
        document.setSource(documentSource);
        document.setStatus("PROCESSING");
        document.setDocVersion(1);
        document.setIndexStatus("PENDING");
        document.setCreatedAt(now);
        document.setUpdatedAt(now);
        kbDocumentMapper.insert(document);

        persistChunks(principal.tenantId(), document.getDocumentId(), 1, chunks, now);
        enqueueOutbox(principal.tenantId(), document.getDocumentId(), 1, "UPSERT", now);
        return toDto(document);
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

    public List<DocumentDto> listDocuments(AuthPrincipal principal) {
        return kbDocumentMapper.selectList(
                        new LambdaQueryWrapper<KbDocumentEntity>()
                                .eq(KbDocumentEntity::getTenantId, principal.tenantId())
                                .eq(KbDocumentEntity::getOwnerUserId, principal.userId())
                                .ne(KbDocumentEntity::getStatus, "DELETED")
                                .orderByDesc(KbDocumentEntity::getUpdatedAt)
                ).stream()
                .map(this::toDto)
                .toList();
    }

    public List<ChunkDto> listChunks(AuthPrincipal principal, Long documentId) {
        KbDocumentEntity document = requireDocument(principal, documentId);
        return kbChunkMapper.selectByDocVersion(principal.tenantId(), documentId, document.getDocVersion())
                .stream()
                .map(chunk -> {
                    ChunkDto dto = new ChunkDto();
                    dto.setChunkId(chunk.getChunkId());
                    dto.setChunkOrder(chunk.getChunkOrder());
                    dto.setContent(chunk.getContent());
                    return dto;
                })
                .collect(toList());
    }

    @Transactional
    public Map<String, Object> resetForEvaluation(AuthPrincipal principal, boolean purgeDb) {
        ensureElasticsearchEnabled();
        elasticsearchChunkStore.deleteKbChunksIndex();
        if (purgeDb) {
            int deletedOutbox = kbIndexOutboxMapper.delete(
                    new LambdaQueryWrapper<KbIndexOutboxEntity>()
                            .eq(KbIndexOutboxEntity::getTenantId, principal.tenantId())
            );
            int deletedChunks = kbChunkMapper.delete(
                    new LambdaQueryWrapper<KbChunkEntity>()
                            .eq(KbChunkEntity::getTenantId, principal.tenantId())
            );
            int deletedDocuments = kbDocumentMapper.delete(
                    new LambdaQueryWrapper<KbDocumentEntity>()
                            .eq(KbDocumentEntity::getTenantId, principal.tenantId())
                            .eq(KbDocumentEntity::getOwnerUserId, principal.userId())
            );
            return Map.of(
                    "indexDeleted", true,
                    "purgeDb", true,
                    "deletedDocuments", deletedDocuments,
                    "deletedChunks", deletedChunks,
                    "deletedOutbox", deletedOutbox
            );
        }
        return Map.of("indexDeleted", true, "purgeDb", false);
    }


    @Transactional
    public DocumentDto triggerIndex(AuthPrincipal principal, Long documentId, String requestId) {
        ensureElasticsearchEnabled();
        idempotencyService.ensureUnique(principal, "knowledge:trigger-index", requestId);
        KbDocumentEntity document = requireDocument(principal, documentId);
        if ("DELETED".equalsIgnoreCase(document.getStatus())) {
            throw AppException.badRequest("已删除文档无法触发索引");
        }

        int fromVersion = document.getDocVersion();
        int nextVersion = fromVersion + 1;
        int copied = kbChunkMapper.copyVersion(principal.tenantId(), documentId, fromVersion, nextVersion);
        if (copied <= 0) {
            throw AppException.badRequest("文档暂无可索引内容，请先导入文件");
        }

        document.setDocVersion(nextVersion);
        document.setStatus("PROCESSING");
        document.setIndexStatus("PENDING");
        document.setUpdatedAt(LocalDateTime.now());
        kbDocumentMapper.updateById(document);

        enqueueOutbox(principal.tenantId(), documentId, nextVersion, "UPSERT", LocalDateTime.now());
        return toDto(document);
    }

    @Transactional
    public DocumentDto deleteDocument(AuthPrincipal principal, Long documentId, String requestId) {
        ensureElasticsearchEnabled();
        idempotencyService.ensureUnique(principal, "knowledge:delete-document", requestId);
        KbDocumentEntity document = requireDocument(principal, documentId);
        if ("DELETED".equalsIgnoreCase(document.getStatus())) {
            return toDto(document);
        }

        int nextVersion = document.getDocVersion() + 1;
        document.setDocVersion(nextVersion);
        document.setStatus("DELETED");
        document.setIndexStatus("PENDING");
        document.setUpdatedAt(LocalDateTime.now());
        kbDocumentMapper.updateById(document);

        enqueueOutbox(principal.tenantId(), documentId, nextVersion, "DELETE", LocalDateTime.now());
        return toDto(document);
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
                               String op,
                               LocalDateTime now) {
        KbIndexOutboxEntity outbox = new KbIndexOutboxEntity();
        outbox.setTenantId(tenantId);
        outbox.setDocumentId(documentId);
        outbox.setDocVersion(docVersion);
        outbox.setOp(op);
        outbox.setStatus("PENDING");
        outbox.setRetryCount(0);
        outbox.setNextRetryAt(null);
        outbox.setCreatedAt(now);
        outbox.setUpdatedAt(now);
        kbIndexOutboxMapper.insert(outbox);
    }

    private KbDocumentEntity requireDocument(AuthPrincipal principal, Long documentId) {
        KbDocumentEntity document = kbDocumentMapper.selectOne(
                new LambdaQueryWrapper<KbDocumentEntity>()
                        .eq(KbDocumentEntity::getDocumentId, documentId)
                        .eq(KbDocumentEntity::getTenantId, principal.tenantId())
                        .eq(KbDocumentEntity::getOwnerUserId, principal.userId())
                        .last("limit 1")
        );
        if (document == null) {
            throw AppException.notFound("文档不存在");
        }
        return document;
    }

    private void ensureElasticsearchEnabled() {
        if (!elasticsearchChunkStore.isEnabled()) {
            throw AppException.badRequest("当前环境未启用 Elasticsearch，无法执行索引相关操作");
        }
    }

    private String fallbackTitle(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return "未命名文档";
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
        dto.setStatus(entity.getStatus());
        dto.setIndexStatus(entity.getIndexStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
