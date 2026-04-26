package com.legal.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AskRequest {

    @NotBlank(message = "sessionId 不能为空")
    private String sessionId;

    @NotBlank(message = "question 不能为空")
    @Size(max = 2000, message = "question 长度不能超过 2000")
    private String question;

    @NotBlank(message = "requestId 不能为空")
    private String requestId;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}
