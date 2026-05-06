package com.legal.court.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.legal.common.AppException;
import com.legal.court.dto.CourtJudgeOutput;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 智能小法庭 AI 法官输出校验器。
 */
@Component
public class CourtJudgeOutputValidator {

    private final ObjectMapper objectMapper;

    public CourtJudgeOutputValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 解析并校验法官输出，要求双向不利点均非空。
     */
    public CourtJudgeOutput validate(String rawText) {
        if (!StringUtils.hasText(rawText)) {
            throw AppException.badRequest("法官输出为空");
        }
        try {
            CourtJudgeOutput output = objectMapper.readValue(rawText, CourtJudgeOutput.class);
            validateRequiredFields(output);
            return output;
        } catch (AppException ex) {
            throw ex;
        } catch (Exception ex) {
            throw AppException.badRequest("法官输出不是合法 JSON");
        }
    }

    private void validateRequiredFields(CourtJudgeOutput output) {
        if (output.getFocusIssues() == null
                || output.getAcceptedFacts() == null
                || output.getRejectedFacts() == null
                || output.getUnfavorableToPartyA() == null
                || output.getUnfavorableToPartyB() == null
                || output.getOpenQuestions() == null) {
            throw AppException.badRequest("法官输出缺少必要字段");
        }
        if (output.getUnfavorableToPartyA().isEmpty() || output.getUnfavorableToPartyB().isEmpty()) {
            throw AppException.badRequest("法官输出缺少双向不利点");
        }
    }
}
