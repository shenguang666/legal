package com.legal.knowledge.service;

import com.legal.config.DocumentProcessingProperties;
import com.legal.enums.DocumentParseMethod;
import com.legal.knowledge.dto.DocumentProcessingCapabilitiesDto;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class DocumentProcessingCapabilityService {

    private final DocumentProcessingProperties properties;

    public DocumentProcessingCapabilityService(DocumentProcessingProperties properties) {
        this.properties = properties;
    }

    public DocumentProcessingCapabilitiesDto capabilities() {
        DocumentProcessingCapabilitiesDto dto = new DocumentProcessingCapabilitiesDto();
        dto.setDefaultParseMethod(resolveDefaultParseMethod().getCode());
        dto.setMaxUploadDocuments(properties.getMaxUploadDocuments());
        dto.setAvailableParseMethods(availableParseMethods().stream().map(DocumentParseMethod::getCode).toList());
        return dto;
    }

    public List<DocumentParseMethod> availableParseMethods() {
        List<DocumentParseMethod> methods = new ArrayList<>();
        methods.add(DocumentParseMethod.NATIVE);
        if (isMineruAvailable()) {
            methods.add(DocumentParseMethod.MINERU_PRECISE);
        }
        return methods;
    }

    public DocumentParseMethod resolveParseMethod(String value) {
        DocumentParseMethod method = StringUtils.hasText(value) ? parseMethod(value) : resolveDefaultParseMethod();
        if (method == DocumentParseMethod.MINERU_PRECISE && !isMineruAvailable()) {
            return DocumentParseMethod.NATIVE;
        }
        return method;
    }

    public void validateUploadCount(int count) {
        int max = Math.max(1, properties.getMaxUploadDocuments());
        if (count > max) {
            throw com.legal.common.AppException.badRequest("单次最多上传 " + max + " 个文档");
        }
    }

    public boolean isMineruAvailable() {
        return properties.getMineru().isEnabled() && StringUtils.hasText(properties.getMineru().getApiToken());
    }

    private DocumentParseMethod resolveDefaultParseMethod() {
        DocumentParseMethod configured = properties.getDefaultParseMethod() == null
                ? DocumentParseMethod.NATIVE
                : properties.getDefaultParseMethod();
        if (configured == DocumentParseMethod.MINERU_PRECISE && !isMineruAvailable()) {
            return DocumentParseMethod.NATIVE;
        }
        return configured;
    }

    private DocumentParseMethod parseMethod(String value) {
        try {
            return DocumentParseMethod.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw com.legal.common.AppException.badRequest("不支持的文档解析方式: " + value);
        }
    }
}
