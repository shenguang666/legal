package com.legal.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateDocumentRequest {

    @NotBlank(message = "requestId 不能为空")
    private String requestId;

    @NotBlank(message = "title 不能为空")
    @Size(max = 255, message = "title 长度不能超过 255")
    private String title;

    @NotBlank(message = "source 不能为空")
    @Size(max = 255, message = "source 长度不能超过 255")
    private String source;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
