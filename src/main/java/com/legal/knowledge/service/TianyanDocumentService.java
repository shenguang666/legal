package com.legal.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.legal.common.AppException;
import com.legal.enums.KbDocumentBizType;
import com.legal.enums.KbDocumentStatus;
import com.legal.enums.KbIndexStatus;
import com.legal.knowledge.dto.DocumentDto;
import com.legal.knowledge.entity.KbChunkEntity;
import com.legal.knowledge.entity.KbDocumentEntity;
import com.legal.knowledge.mapper.KbChunkMapper;
import com.legal.knowledge.mapper.KbDocumentMapper;
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
public class TianyanDocumentService {

    private final KbDocumentMapper kbDocumentMapper;
    private final KbChunkMapper kbChunkMapper;
    private final IdempotencyService idempotencyService;
    private final DocumentTextExtractor documentTextExtractor;
    private final DocumentChunker documentChunker;

    public TianyanDocumentService(KbDocumentMapper kbDocumentMapper,
                                  KbChunkMapper kbChunkMapper,
                                  IdempotencyService idempotencyService,
                                  DocumentTextExtractor documentTextExtractor,
                                  DocumentChunker documentChunker) {
        this.kbDocumentMapper = kbDocumentMapper;
        this.kbChunkMapper = kbChunkMapper;
        this.idempotencyService = idempotencyService;
        this.documentTextExtractor = documentTextExtractor;
        this.documentChunker = documentChunker;
    }

    @Transactional
    public DocumentDto importDocument(AuthPrincipal principal,
                                      String requestId,
                                      MultipartFile file,
                                      String title,
                                      String source,
                                      Integer chunkSize,
                                      Integer chunkOverlap) {
        idempotencyService.ensureUnique(principal, "tianyan:import-document", requestId);
        String extractedText = documentTextExtractor.extract(file);
        List<String> chunks = buildChunks(extractedText, chunkSize, chunkOverlap);
        if (chunks.isEmpty()) {
            throw AppException.badRequest("文档内容过短，无法生成天眼审查切片");
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
        document.setBizType(KbDocumentBizType.TIANYAN_REVIEW);
        document.setStatus(KbDocumentStatus.ACTIVE);
        document.setDocVersion(1);
        document.setIndexStatus(KbIndexStatus.COMPLETED);
        document.setCreatedAt(now);
        document.setUpdatedAt(now);
        kbDocumentMapper.insert(document);

        persistChunks(principal.tenantId(), document.getDocumentId(), 1, chunks, now);
        return toDto(document);
    }

    public List<DocumentDto> listDocuments(AuthPrincipal principal) {
        return kbDocumentMapper.selectList(
                        new LambdaQueryWrapper<KbDocumentEntity>()
                                .eq(KbDocumentEntity::getTenantId, principal.tenantId())
                                .eq(KbDocumentEntity::getOwnerUserId, principal.userId())
                                .eq(KbDocumentEntity::getBizType, KbDocumentBizType.TIANYAN_REVIEW)
                                .ne(KbDocumentEntity::getStatus, KbDocumentStatus.DELETED)
                                .orderByDesc(KbDocumentEntity::getUpdatedAt)
                ).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public DocumentDto deleteDocument(AuthPrincipal principal, Long documentId, String requestId) {
        idempotencyService.ensureUnique(principal, "tianyan:delete-document", requestId);
        KbDocumentEntity document = requireDocument(principal, documentId);
        if (document.getStatus() == KbDocumentStatus.DELETED) {
            return toDto(document);
        }
        document.setStatus(KbDocumentStatus.DELETED);
        document.setUpdatedAt(LocalDateTime.now());
        kbDocumentMapper.updateById(document);
        return toDto(document);
    }

    private KbDocumentEntity requireDocument(AuthPrincipal principal, Long documentId) {
        KbDocumentEntity document = kbDocumentMapper.selectOne(
                new LambdaQueryWrapper<KbDocumentEntity>()
                        .eq(KbDocumentEntity::getDocumentId, documentId)
                        .eq(KbDocumentEntity::getTenantId, principal.tenantId())
                        .eq(KbDocumentEntity::getOwnerUserId, principal.userId())
                        .eq(KbDocumentEntity::getBizType, KbDocumentBizType.TIANYAN_REVIEW)
                        .last("limit 1")
        );
        if (document == null) {
            throw AppException.notFound("天眼审查文档不存在");
        }
        return document;
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

    private String fallbackTitle(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return "未命名审查文档";
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
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
