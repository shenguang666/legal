package com.legal.knowledge.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legal.common.AppException;
import com.legal.config.DocumentProcessingProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class MineruClient {

    private final DocumentProcessingProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public MineruClient(DocumentProcessingProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getMineru().getConnectTimeout());
        requestFactory.setReadTimeout(properties.getMineru().getReadTimeout());
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl(trimTrailingSlash(properties.getMineru().getBaseUrl()))
                .build();
    }

    public MineruUploadSession submit(String fileName, byte[] bytes) {
        if (!StringUtils.hasText(properties.getMineru().getApiToken())) {
            throw AppException.badRequest("MinerU Token 未配置，无法使用 MinerU 精准解析");
        }
        String dataId = UUID.randomUUID().toString().replace("-", "");
        MineruFileUrlRequest payload = new MineruFileUrlRequest(
                List.of(new MineruFileDescriptor(fileName, dataId)),
                properties.getMineru().getModelVersion()
        );
        JsonNode response = postFileUrlsJson(payload);
        validateMineruSuccess(response, "申请 MinerU 上传地址失败");
        JsonNode dataNode = response == null ? null : response.path("data");
        String batchId = firstText(dataNode, "batch_id", "batchId");
        String uploadUrl = firstFileUrl(dataNode);
        if (!StringUtils.hasText(batchId)) {
            batchId = firstText(response, "batch_id", "batchId");
        }
        if (!StringUtils.hasText(uploadUrl)) {
            uploadUrl = findUploadUrl(response, dataId);
        }
        if (!StringUtils.hasText(batchId) || !StringUtils.hasText(uploadUrl)) {
            throw AppException.badRequest("MinerU 未返回有效上传地址");
        }
        uploadFile(uploadUrl, bytes);
        return new MineruUploadSession(batchId, dataId);
    }

    public MineruExtractResult waitForResult(String batchId, String dataId) {
        Instant deadline = Instant.now().plus(properties.getMineru().getTimeout());
        while (Instant.now().isBefore(deadline)) {
            MineruExtractResult result = queryResult(batchId, dataId);
            if (result.done() || result.failed()) {
                return result;
            }
            sleep(properties.getMineru().getPollInterval().toMillis());
        }
        return MineruExtractResult.failed("MinerU 解析超时");
    }

    public String downloadMarkdown(String fullZipUrl) {
        return downloadPackage(fullZipUrl).markdown();
    }

    public MineruParsePackage downloadPackage(String fullZipUrl) {
        if (!StringUtils.hasText(fullZipUrl)) {
            throw AppException.badRequest("MinerU 未返回解析结果下载地址");
        }
        try {
            byte[] zipBytes = RestClient.create()
                    .get()
                    .uri(fullZipUrl)
                    .retrieve()
                    .body(byte[].class);
            MineruParsePackage parsePackage = readPackageFromZip(zipBytes);
            if (!StringUtils.hasText(parsePackage.markdown())) {
                throw AppException.badRequest("MinerU 解析结果中未找到 Markdown 内容");
            }
            return parsePackage;
        } catch (RestClientException ex) {
            throw AppException.badRequest("下载 MinerU 解析结果失败");
        }
    }

    private MineruExtractResult queryResult(String batchId, String dataId) {
        JsonNode response = getJson("/api/v4/extract-results/batch/" + batchId);
        JsonNode resultNode = findResultNode(response, dataId);
        String status = firstText(resultNode == null ? response : resultNode, "state", "status", "extract_state");
        String normalized = status == null ? "" : status.trim().toLowerCase();
        String fullZipUrl = firstText(resultNode == null ? response : resultNode, "full_zip_url", "fullZipUrl", "zip_url", "zipUrl");
        if (List.of("done", "success", "completed", "finish", "finished").contains(normalized)) {
            return MineruExtractResult.done(fullZipUrl);
        }
        if (List.of("failed", "fail", "error").contains(normalized)) {
            String message = firstText(resultNode == null ? response : resultNode, "err_msg", "error", "message", "fail_reason");
            return MineruExtractResult.failed(StringUtils.hasText(message) ? message : "MinerU 解析失败");
        }
        return MineruExtractResult.processing();
    }

    private JsonNode postJson(String path, Object payload) {
        String jsonPayload = toJsonPayload(payload);
        try {
            return restClient.post()
                    .uri(path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getMineru().getApiToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonPayload)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException ex) {
            throw mineruRequestException("调用 MinerU 创建解析任务失败", ex);
        }
    }

    private JsonNode postFileUrlsJson(Object payload) {
        String jsonPayload = toJsonPayload(payload);
        try {
            return restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v4/file-urls/batch")
                            .queryParam("enable_formula", properties.getMineru().isEnableFormula())
                            .queryParam("enable_table", properties.getMineru().isEnableTable())
                            .queryParam("language", properties.getMineru().getLanguage())
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getMineru().getApiToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonPayload)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException ex) {
            throw mineruRequestException("申请 MinerU 上传地址失败", ex);
        }
    }

    private JsonNode getJson(String path) {
        try {
            return restClient.get()
                    .uri(path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getMineru().getApiToken())
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException ex) {
            throw mineruRequestException("查询 MinerU 解析结果失败", ex);
        }
    }

    private void uploadFile(String uploadUrl, byte[] bytes) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(uploadUrl))
                    .timeout(properties.getMineru().getReadTimeout())
                    .PUT(HttpRequest.BodyPublishers.ofByteArray(bytes))
                    .build();
            HttpResponse<Void> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw AppException.badRequest("上传文件到 MinerU 失败：HTTP " + response.statusCode());
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw AppException.badRequest("上传文件到 MinerU 失败：" + truncateError(ex.getMessage()));
        } catch (IOException | IllegalArgumentException ex) {
            throw AppException.badRequest("上传文件到 MinerU 失败：" + truncateError(ex.getMessage()));
        }
    }

    private AppException mineruRequestException(String action, RestClientException ex) {
        String detail = ex.getMessage();
        if (ex instanceof RestClientResponseException responseEx) {
            String body = responseEx.getResponseBodyAsString();
            String status = "HTTP " + responseEx.getStatusCode().value();
            if (StringUtils.hasText(responseEx.getStatusText())) {
                status = status + " " + responseEx.getStatusText();
            }
            detail = StringUtils.hasText(body) ? status + "，" + body : status;
        }
        return AppException.badRequest(StringUtils.hasText(detail) ? action + "：" + truncateError(detail) : action);
    }

    private String toJsonPayload(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw AppException.badRequest("构造 MinerU 请求体失败：" + truncateError(ex.getMessage()));
        }
    }

    private String truncateError(String message) {
        if (!StringUtils.hasText(message)) {
            return "";
        }
        String value = message.trim();
        return value.length() > 1000 ? value.substring(0, 1000) : value;
    }

    private void validateMineruSuccess(JsonNode response, String fallbackMessage) {
        if (response == null || response.isNull() || response.isMissingNode()) {
            throw AppException.badRequest(fallbackMessage);
        }
        JsonNode codeNode = response.get("code");
        if (codeNode == null || codeNode.isNull() || codeNode.isMissingNode() || "0".equals(codeNode.asText())) {
            return;
        }
        String message = firstText(response, "msg", "message", "error", "err_msg");
        throw AppException.badRequest(StringUtils.hasText(message) ? fallbackMessage + "：" + message : fallbackMessage);
    }

    private String firstFileUrl(JsonNode dataNode) {
        String directUrl = firstText(dataNode, "upload_url", "uploadUrl", "file_url", "fileUrl", "url");
        if (isHttpUrl(directUrl)) {
            return directUrl;
        }
        for (String name : List.of("file_urls", "fileUrls", "upload_urls", "uploadUrls", "urls")) {
            String found = firstUrlFromNode(findField(dataNode, name));
            if (StringUtils.hasText(found)) {
                return found;
            }
        }
        return null;
    }

    private String firstUrlFromNode(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        if (node.isTextual() && isHttpUrl(node.asText())) {
            return node.asText();
        }
        if (node.isArray() || node.isObject()) {
            Iterator<JsonNode> values = node.elements();
            while (values.hasNext()) {
                String found = firstUrlFromNode(values.next());
                if (StringUtils.hasText(found)) {
                    return found;
                }
            }
        }
        return null;
    }

    private String readMarkdownFromZip(byte[] zipBytes) {
        return readPackageFromZip(zipBytes).markdown();
    }

    private MineruParsePackage readPackageFromZip(byte[] zipBytes) {
        if (zipBytes == null || zipBytes.length == 0) {
            return new MineruParsePackage(null, List.of());
        }
        List<Map.Entry<String, String>> markdownEntries = new ArrayList<>();
        List<MineruImageEntry> imageEntries = new ArrayList<>();
        List<MineruPackageFile> packageFiles = new ArrayList<>();
        Set<String> supportedExtensions = properties.getMineru().getImageAsset().getSupportedExtensions() == null
                ? Set.of()
                : properties.getMineru().getImageAsset().getSupportedExtensions().stream()
                .filter(StringUtils::hasText)
                .map(extension -> extension.toLowerCase(Locale.ROOT).replace(".", ""))
                .collect(java.util.stream.Collectors.toSet());
        int maxImages = Math.max(0, properties.getMineru().getImageAsset().getMaxImagesPerDocument());
        long totalImageBytes = 0L;
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(zipBytes), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String normalizedPath = normalizeZipPath(entry.getName());
                if (!StringUtils.hasText(normalizedPath)) {
                    continue;
                }
                byte[] entryBytes = readAllEntryBytes(zipInputStream);
                packageFiles.add(new MineruPackageFile(entry.getName(), normalizedPath, entryBytes, mimeTypeByPath(normalizedPath)));
                String lowerName = normalizedPath.toLowerCase(Locale.ROOT);
                if (lowerName.endsWith(".md")) {
                    markdownEntries.add(Map.entry(normalizedPath, new String(entryBytes, StandardCharsets.UTF_8)));
                    continue;
                }
                String extension = extensionOf(lowerName);
                if (!supportedExtensions.contains(extension) || imageEntries.size() >= maxImages) {
                    continue;
                }
                long remainingTotal = properties.getMineru().getImageAsset().getMaxTotalImageBytes() - totalImageBytes;
                long maxImageBytes = Math.min(properties.getMineru().getImageAsset().getMaxImageBytes(), remainingTotal);
                if (maxImageBytes <= 0) {
                    continue;
                }
                if (entryBytes.length > maxImageBytes) {
                    continue;
                }
                totalImageBytes += entryBytes.length;
                imageEntries.add(new MineruImageEntry(
                        entry.getName(),
                        normalizedPath,
                        entryBytes,
                        extension,
                        mimeType(extension)
                ));
            }
        } catch (IOException ex) {
            throw AppException.badRequest("读取 MinerU 解析结果失败");
        }
        String markdown = markdownEntries.stream()
                .filter(entry -> entry.getKey().toLowerCase(Locale.ROOT).endsWith("full.md"))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(markdownEntries.isEmpty() ? null : markdownEntries.get(0).getValue());
        String markdownPath = markdownEntries.stream()
                .filter(entry -> entry.getKey().toLowerCase(Locale.ROOT).endsWith("full.md"))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(markdownEntries.isEmpty() ? null : markdownEntries.get(0).getKey());
        return new MineruParsePackage(markdown, imageEntries, packageFiles, markdownPath);
    }

    private byte[] readAllEntryBytes(ZipInputStream zipInputStream) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = zipInputStream.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    static String normalizeZipPath(String path) {
        if (!StringUtils.hasText(path)) {
            return null;
        }
        String normalized = URLDecoder.decode(path.trim().replace('\\', '/'), StandardCharsets.UTF_8);
        while (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        if (normalized.startsWith("/") || normalized.contains("../") || normalized.equals("..") || normalized.startsWith("..")) {
            return null;
        }
        return normalized;
    }

    private String extensionOf(String path) {
        int index = path.lastIndexOf('.');
        if (index < 0 || index == path.length() - 1) {
            return "";
        }
        return path.substring(index + 1);
    }

    private String mimeType(String extension) {
        return switch (extension) {
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG_VALUE;
            case "png" -> MediaType.IMAGE_PNG_VALUE;
            case "webp" -> "image/webp";
            default -> MediaType.APPLICATION_OCTET_STREAM_VALUE;
        };
    }

    private String mimeTypeByPath(String path) {
        String extension = extensionOf(path == null ? "" : path.toLowerCase(Locale.ROOT));
        return switch (extension) {
            case "md" -> "text/markdown; charset=utf-8";
            case "txt" -> MediaType.TEXT_PLAIN_VALUE;
            case "json" -> MediaType.APPLICATION_JSON_VALUE;
            case "html", "htm" -> MediaType.TEXT_HTML_VALUE;
            case "pdf" -> MediaType.APPLICATION_PDF_VALUE;
            default -> mimeType(extension);
        };
    }

    private JsonNode findResultNode(JsonNode response, String dataId) {
        if (response == null) {
            return null;
        }
        List<JsonNode> arrays = new ArrayList<>();
        collectArrays(response, arrays);
        for (JsonNode array : arrays) {
            for (JsonNode item : array) {
                String itemDataId = firstText(item, "data_id", "dataId");
                if (!StringUtils.hasText(dataId) || dataId.equals(itemDataId)) {
                    return item;
                }
            }
        }
        return response;
    }

    private String findUploadUrl(JsonNode response, String dataId) {
        String directUploadUrl = firstText(response, "upload_url", "uploadUrl", "file_url", "fileUrl", "url");
        if (isHttpUrl(directUploadUrl)) {
            return directUploadUrl;
        }
        List<JsonNode> arrays = new ArrayList<>();
        collectArrays(response, arrays);
        for (JsonNode array : arrays) {
            for (JsonNode item : array) {
                if (item != null && item.isTextual() && isHttpUrl(item.asText())) {
                    return item.asText();
                }
                String itemDataId = firstText(item, "data_id", "dataId");
                String uploadUrl = firstText(item, "upload_url", "uploadUrl", "file_url", "fileUrl", "url");
                if (isHttpUrl(uploadUrl) && (!StringUtils.hasText(itemDataId) || dataId.equals(itemDataId))) {
                    return uploadUrl;
                }
            }
        }
        return findHttpUrl(response);
    }

    private void collectArrays(JsonNode node, List<JsonNode> arrays) {
        if (node == null) {
            return;
        }
        if (node.isArray()) {
            arrays.add(node);
            return;
        }
        if (node.isObject()) {
            Iterator<JsonNode> values = node.elements();
            while (values.hasNext()) {
                collectArrays(values.next(), arrays);
            }
        }
    }

    private String firstText(JsonNode node, String... names) {
        if (node == null) {
            return null;
        }
        for (String name : names) {
            JsonNode found = findField(node, name);
            if (found != null && found.isValueNode() && StringUtils.hasText(found.asText())) {
                return found.asText();
            }
        }
        return null;
    }

    private String findHttpUrl(JsonNode node) {
        if (node == null) {
            return null;
        }
        if (node.isTextual() && isHttpUrl(node.asText())) {
            return node.asText();
        }
        if (node.isObject() || node.isArray()) {
            Iterator<JsonNode> values = node.elements();
            while (values.hasNext()) {
                String found = findHttpUrl(values.next());
                if (StringUtils.hasText(found)) {
                    return found;
                }
            }
        }
        return null;
    }

    private boolean isHttpUrl(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String text = value.trim().toLowerCase();
        return text.startsWith("http://") || text.startsWith("https://");
    }

    private JsonNode findField(JsonNode node, String name) {
        if (node == null) {
            return null;
        }
        JsonNode direct = node.get(name);
        if (direct != null) {
            return direct;
        }
        if (node.isObject() || node.isArray()) {
            Iterator<JsonNode> values = node.elements();
            while (values.hasNext()) {
                JsonNode found = findField(values.next(), name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(Math.max(500L, millis));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw AppException.badRequest("MinerU 解析轮询被中断");
        }
    }

    private String trimTrailingSlash(String value) {
        String text = StringUtils.hasText(value) ? value.trim() : "https://mineru.net";
        while (text.endsWith("/")) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }

    public record MineruUploadSession(String batchId, String dataId) {
    }

    private record MineruFileUrlRequest(List<MineruFileDescriptor> files, String model_version) {
    }

    private record MineruFileDescriptor(String name, String data_id) {
    }

    public record MineruExtractResult(boolean done, boolean failed, String fullZipUrl, String errorMessage) {

        public static MineruExtractResult processing() {
            return new MineruExtractResult(false, false, null, null);
        }

        public static MineruExtractResult done(String fullZipUrl) {
            return new MineruExtractResult(true, false, fullZipUrl, null);
        }

        public static MineruExtractResult failed(String errorMessage) {
            return new MineruExtractResult(false, true, null, errorMessage);
        }
    }
}
