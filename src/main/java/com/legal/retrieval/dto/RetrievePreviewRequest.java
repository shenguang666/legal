package com.legal.retrieval.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class RetrievePreviewRequest {

    @NotBlank(message = "question 不能为空")
    private String question;

    @Min(value = 1, message = "topK 必须大于 0")
    private Integer topK;

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public Integer getTopK() {
        return topK;
    }

    public void setTopK(Integer topK) {
        this.topK = topK;
    }
}
