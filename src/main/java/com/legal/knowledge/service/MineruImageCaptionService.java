package com.legal.knowledge.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.legal.config.DocumentProcessingProperties;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class MineruImageCaptionService {

    private static final String DEFAULT_DESCRIPTION = "图片";

    private final DocumentProcessingProperties properties;

    public MineruImageCaptionService(DocumentProcessingProperties properties) {
        this.properties = properties;
    }

    public boolean isEnabled() {
        DocumentProcessingProperties.ImageCaption caption = properties.getMineru().getImageCaption();
        return caption.isEnabled()
                && StringUtils.hasText(caption.getBaseUrl())
                && StringUtils.hasText(caption.getApiKey())
                && StringUtils.hasText(caption.getModelName());
    }

    public boolean shouldSkipCaption(MineruImageEntry image) {
        long minBytes = properties.getMineru().getImageAsset().getMinMeaningfulImageBytes();
        return image == null || image.bytes() == null || image.bytes().length < minBytes;
    }

    public String describe(MineruImageEntry image) {
        if (!isEnabled() || shouldSkipCaption(image)) {
            return DEFAULT_DESCRIPTION;
        }
        DocumentProcessingProperties.ImageCaption caption = properties.getMineru().getImageCaption();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(caption.getTimeout());
        requestFactory.setReadTimeout(caption.getTimeout());
        RestClient restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl(trimTrailingSlash(caption.getBaseUrl()))
                .build();
        String dataUrl = "data:" + image.mimeType() + ";base64," + Base64.getEncoder().encodeToString(image.bytes());
        Map<String, Object> body = Map.of(
                "model", caption.getModelName(),
                "max_tokens", caption.getMaxOutputTokens(),
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", List.of(
                                Map.of("type", "text", "text", "请用中文客观描述这张图片在法律、法规或合同文档中的可检索含义。如果是图标、网站装饰、二维码、页眉页脚或无业务含义图片，请回答“装饰性图片”。如果包含表格、流程图、盖章、签字、截图、证据材料，请描述关键文字、主体、金额、日期、关系和风险点。回答控制在80字以内。"),
                                Map.of("type", "image_url", "image_url", Map.of("url", dataUrl))
                        )
                ))
        );
        try {
            JsonNode response = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + caption.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            String text = response == null ? null : response.path("choices").path(0).path("message").path("content").asText();
            return StringUtils.hasText(text) ? text.trim() : DEFAULT_DESCRIPTION;
        } catch (RestClientException ex) {
            return DEFAULT_DESCRIPTION;
        }
    }

    public boolean isDecorativeDescription(String description) {
        return StringUtils.hasText(description) && description.contains("装饰性图片");
    }

    private String trimTrailingSlash(String value) {
        String result = value == null ? "" : value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
