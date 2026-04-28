package com.legal.knowledge.service;

import com.legal.common.AppException;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;

@Component
public class DocumentTextExtractor {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("pdf", "doc", "docx", "txt", "md");
    private static final long MAX_FILE_SIZE_BYTES = 20L * 1024L * 1024L;

    private final Tika tika = new Tika();

    public String extract(MultipartFile file) {
        validateFile(file);
        try (InputStream inputStream = file.getInputStream()) {
            String text = tika.parseToString(inputStream);
            String normalized = text == null ? "" : text.replace("\u0000", "").trim();
            if (!StringUtils.hasText(normalized)) {
                throw AppException.badRequest("上传文档内容为空，无法建立索引");
            }
            return normalized;
        } catch (IOException ex) {
            throw new AppException(50020, 500, "解析文档失败: " + ex.getMessage());
        } catch (TikaException e) {
            throw new RuntimeException(e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw AppException.badRequest("请上传文档文件");
        }
        if (file.getSize() <= 0) {
            throw AppException.badRequest("文档大小不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw AppException.badRequest("文档大小不能超过 20MB");
        }
        String fileName = file.getOriginalFilename();
        String extension = getExtension(fileName);
        if (!StringUtils.hasText(extension) || !SUPPORTED_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT))) {
            throw AppException.badRequest("仅支持 pdf/doc/docx/txt/md 格式文档");
        }
    }

    private String getExtension(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return "";
        }
        int idx = fileName.lastIndexOf('.');
        if (idx < 0 || idx == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(idx + 1);
    }
}
