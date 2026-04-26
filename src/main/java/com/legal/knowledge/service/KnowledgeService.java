package com.legal.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.legal.common.AppException;
import com.legal.knowledge.dto.CreateDocumentRequest;
import com.legal.knowledge.dto.DocumentDto;
import com.legal.knowledge.entity.KbDocumentEntity;
import com.legal.knowledge.entity.KbIndexOutboxEntity;
import com.legal.knowledge.mapper.KbDocumentMapper;
import com.legal.knowledge.mapper.KbIndexOutboxMapper;
import com.legal.security.AuthPrincipal;
import com.legal.security.IdempotencyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class KnowledgeService {

    private final KbDocumentMapper kbDocumentMapper;
    private final KbIndexOutboxMapper kbIndexOutboxMapper;
    private final IdempotencyService idempotencyService;

    public KnowledgeService(KbDocumentMapper kbDocumentMapper,
                            KbIndexOutboxMapper kbIndexOutboxMapper,
                            IdempotencyService idempotencyService) {
        this.kbDocumentMapper = kbDocumentMapper;
        this.kbIndexOutboxMapper = kbIndexOutboxMapper;
        this.idempotencyService = idempotencyService;
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

    @Transactional
    public DocumentDto triggerIndex(AuthPrincipal principal, Long documentId, String requestId) {
        idempotencyService.ensureUnique(principal, "knowledge:trigger-index", requestId);
        KbDocumentEntity document = requireDocument(principal, documentId);
        int nextVersion = document.getDocVersion() + 1;

        document.setDocVersion(nextVersion);
        document.setStatus("PROCESSING");
        document.setIndexStatus("PROCESSING");
        document.setUpdatedAt(LocalDateTime.now());
        kbDocumentMapper.updateById(document);

        KbIndexOutboxEntity outbox = new KbIndexOutboxEntity();
        outbox.setTenantId(principal.tenantId());
        outbox.setDocumentId(documentId);
        outbox.setDocVersion(nextVersion);
        outbox.setOp("UPSERT");
        outbox.setStatus("PENDING");
        outbox.setRetryCount(0);
        outbox.setCreatedAt(LocalDateTime.now());
        outbox.setUpdatedAt(LocalDateTime.now());
        kbIndexOutboxMapper.insert(outbox);

        return toDto(document);
    }

    @Transactional
    public DocumentDto deleteDocument(AuthPrincipal principal, Long documentId, String requestId) {
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

        KbIndexOutboxEntity outbox = new KbIndexOutboxEntity();
        outbox.setTenantId(principal.tenantId());
        outbox.setDocumentId(documentId);
        outbox.setDocVersion(nextVersion);
        outbox.setOp("DELETE");
        outbox.setStatus("PENDING");
        outbox.setRetryCount(0);
        outbox.setCreatedAt(LocalDateTime.now());
        outbox.setUpdatedAt(LocalDateTime.now());
        kbIndexOutboxMapper.insert(outbox);
        return toDto(document);
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
