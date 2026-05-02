package com.legal.knowledge.service;

import com.legal.common.AppException;
import com.legal.enums.DocumentParseMethod;
import com.legal.enums.DocumentParseStatus;
import com.legal.enums.KbDocumentBizType;
import com.legal.enums.KbDocumentStatus;
import com.legal.enums.KbIndexStatus;
import com.legal.knowledge.entity.KbDocumentEntity;
import com.legal.knowledge.mapper.KbDocumentMapper;
import com.legal.security.AuthPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DocumentImportService {

    private final KbDocumentMapper kbDocumentMapper;
    private final DocumentProcessingCapabilityService capabilityService;
    private final NativeDocumentParser nativeDocumentParser;
    private final DocumentParseTaskService parseTaskService;

    public DocumentImportService(KbDocumentMapper kbDocumentMapper,
                                 DocumentProcessingCapabilityService capabilityService,
                                 NativeDocumentParser nativeDocumentParser,
                                 DocumentParseTaskService parseTaskService) {
        this.kbDocumentMapper = kbDocumentMapper;
        this.capabilityService = capabilityService;
        this.nativeDocumentParser = nativeDocumentParser;
        this.parseTaskService = parseTaskService;
    }

    public KbDocumentEntity importDocument(AuthPrincipal principal,
                                           MultipartFile file,
                                           String title,
                                           String source,
                                           KbDocumentBizType bizType,
                                           String parseMethodValue,
                                           Integer chunkSize,
                                           Integer chunkOverlap,
                                           String emptyChunkMessage) {
        capabilityService.validateUploadCount(1);
        DocumentParseMethod parseMethod = capabilityService.resolveParseMethod(parseMethodValue);
        String originalName = file.getOriginalFilename();
        String documentTitle = StringUtils.hasText(title) ? title.trim() : fallbackTitle(originalName, bizType);
        String documentSource = StringUtils.hasText(source) ? source.trim() : fallbackSource(originalName);
        LocalDateTime now = LocalDateTime.now();

        KbDocumentEntity document = new KbDocumentEntity();
        document.setTenantId(principal.tenantId());
        document.setOwnerUserId(principal.userId());
        document.setTitle(documentTitle);
        document.setSource(documentSource);
        document.setBizType(bizType);
        document.setStatus(parseMethod == DocumentParseMethod.NATIVE ? KbDocumentStatus.PROCESSING : KbDocumentStatus.PENDING);
        document.setDocVersion(1);
        document.setIndexStatus(parseMethod == DocumentParseMethod.NATIVE && bizType == KbDocumentBizType.TIANYAN_REVIEW
                ? KbIndexStatus.COMPLETED
                : KbIndexStatus.PENDING);
        document.setParseMethod(parseMethod);
        document.setParseStatus(parseMethod == DocumentParseMethod.NATIVE ? DocumentParseStatus.PROCESSING : DocumentParseStatus.PENDING);
        document.setParseStartedAt(now);
        document.setCreatedAt(now);
        document.setUpdatedAt(now);
        kbDocumentMapper.insert(document);

        if (parseMethod == DocumentParseMethod.NATIVE) {
            DocumentParseResult result = nativeDocumentParser.parse(new DocumentParseRequest(file, parseMethod, chunkSize, chunkOverlap));
            List<String> chunks = result.getChunks();
            if (chunks.isEmpty()) {
                throw AppException.badRequest(emptyChunkMessage);
            }
            parseTaskService.completeNative(document, chunks);
            return document;
        }

        parseTaskService.createTask(document, fallbackSource(originalName), readBytes(file));
        return document;
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw AppException.badRequest("读取上传文件失败");
        }
    }

    private String fallbackTitle(String fileName, KbDocumentBizType bizType) {
        if (!StringUtils.hasText(fileName)) {
            if (bizType == KbDocumentBizType.RISK_RULE) {
                return "未命名风险规则文档";
            }
            if (bizType == KbDocumentBizType.TIANYAN_REVIEW) {
                return "未命名审查文档";
            }
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
}
