package com.legal.court.agent;

import com.legal.common.AppException;
import com.legal.config.SmartCourtLlmModelSettings;
import com.legal.config.SmartCourtLlmModelSettingsResolver;
import com.legal.config.OpenAiChatModelProperties;
import com.legal.config.SmartCourtProperties;
import com.legal.court.dto.CourtJudgeOutput;
import com.legal.court.dto.CourtJudgeValidationResult;
import com.legal.court.dto.CourtRoleAgentRequest;
import com.legal.court.dto.CourtRoleAgentResult;
import com.legal.enums.CourtArgumentSpeaker;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 智能小法庭多角色 Agent 服务。
 */
@Service
public class CourtRoleAgentService {

    private final ChatModel chatModel;
    private final SmartCourtProperties smartCourtProperties;
    private final OpenAiChatModelProperties fallbackModelProperties;
    private final SmartCourtLlmModelSettingsResolver settingsResolver;
    private final CourtRolePromptTemplateService promptTemplateService;
    private final CourtJudgePerspectiveAnonymizer judgePerspectiveAnonymizer;
    private final CourtJudgeOutputValidator judgeOutputValidator;

    public CourtRoleAgentService(@Nullable @Qualifier("smartCourtChatModel") ChatModel chatModel,
                                 SmartCourtProperties smartCourtProperties,
                                 OpenAiChatModelProperties fallbackModelProperties,
                                 SmartCourtLlmModelSettingsResolver settingsResolver,
                                 CourtRolePromptTemplateService promptTemplateService,
                                 CourtJudgePerspectiveAnonymizer judgePerspectiveAnonymizer,
                                 CourtJudgeOutputValidator judgeOutputValidator) {
        this.chatModel = chatModel;
        this.smartCourtProperties = smartCourtProperties;
        this.fallbackModelProperties = fallbackModelProperties;
        this.settingsResolver = settingsResolver;
        this.promptTemplateService = promptTemplateService;
        this.judgePerspectiveAnonymizer = judgePerspectiveAnonymizer;
        this.judgeOutputValidator = judgeOutputValidator;
    }

    /**
     * 调用 AI 法官 Agent。
     */
    public CourtRoleAgentResult invokeJudge(CourtRoleAgentRequest request) {
        return invoke(CourtArgumentSpeaker.JUDGE, request, buildJudgeUserPrompt(request));
    }

    /**
     * 调用 AI 法官 Agent，并强制校验双向不利点。
     */
    public CourtJudgeValidationResult invokeJudgeWithValidation(CourtRoleAgentRequest request) {
        AppException lastException = null;
        CourtRoleAgentResult lastResult = null;
        int tokenUsage = 0;
        for (int retry = 0; retry <= 2; retry++) {
            lastResult = invokeJudge(request);
            tokenUsage += lastResult.getTokenUsage() == null ? 0 : lastResult.getTokenUsage();
            try {
                CourtJudgeOutput output = judgeOutputValidator.validate(lastResult.getRawText());
                CourtJudgeValidationResult result = new CourtJudgeValidationResult();
                result.setOutput(output);
                result.setRawText(lastResult.getRawText());
                result.setModelName(lastResult.getModelName());
                result.setTokenUsage(tokenUsage);
                result.setValid(true);
                result.setRetryCount(retry);
                return result;
            } catch (AppException ex) {
                lastException = ex;
            }
        }
        CourtJudgeValidationResult fallback = new CourtJudgeValidationResult();
        fallback.setOutput(fallbackJudgeOutput(lastException));
        fallback.setRawText(lastResult == null ? "" : lastResult.getRawText());
        fallback.setModelName(lastResult == null ? null : lastResult.getModelName());
        fallback.setTokenUsage(tokenUsage);
        fallback.setValid(false);
        fallback.setRetryCount(2);
        fallback.setFailureReason(lastException == null ? "法官输出不符合双向不利点要求" : lastException.getMessage());
        return fallback;
    }

    /**
     * 调用 AI 对方代理人 Agent。
     */
    public CourtRoleAgentResult invokeOpponent(CourtRoleAgentRequest request) {
        return invoke(CourtArgumentSpeaker.OPPONENT, request, buildCommonUserPrompt());
    }

    /**
     * 调用 AI 用户辅助律师 Agent。
     */
    public CourtRoleAgentResult invokeUserAdvisor(CourtRoleAgentRequest request) {
        return invoke(CourtArgumentSpeaker.USER_ADVISOR, request, buildCommonUserPrompt());
    }

    private CourtRoleAgentResult invoke(CourtArgumentSpeaker speaker, CourtRoleAgentRequest request, String userPrompt) {
        if (chatModel == null) {
            throw AppException.badRequest("请先配置 LEGAL_SMART_COURT_LLM_API_KEY 或 LEGAL_LLM_API_KEY 后再使用智能小法庭 Agent");
        }
        String systemPrompt = promptTemplateService.render(speaker, request);
        ChatResponse response = chatModel.chat(List.of(SystemMessage.from(systemPrompt), UserMessage.from(userPrompt)));
        CourtRoleAgentResult result = new CourtRoleAgentResult();
        result.setSpeaker(speaker);
        SmartCourtLlmModelSettings settings = settingsResolver.resolve(smartCourtProperties, fallbackModelProperties);
        result.setModelName(settings.modelName());
        result.setRawText(response.aiMessage() == null ? "" : response.aiMessage().text());
        result.setTokenUsage(response.tokenUsage() == null ? 0 : response.tokenUsage().totalTokenCount());
        return result;
    }

    private String buildJudgeUserPrompt(CourtRoleAgentRequest request) {
        return judgePerspectiveAnonymizer.anonymize("请基于系统提示词中的案件材料输出本轮中立法官 JSON。", request.getUserSide());
    }

    private String buildCommonUserPrompt() {
        return "请基于系统提示词中的案件材料输出本轮角色 JSON。";
    }

    private CourtJudgeOutput fallbackJudgeOutput(AppException ex) {
        CourtJudgeOutput output = new CourtJudgeOutput();
        output.getFocusIssues().add("证据不足，无法形成倾向性意见");
        output.getOpenQuestions().add("需补充双方关键证据后重新评估");
        output.getUnfavorableToPartyA().add("现有材料不足以完全支持 PartyA 的全部主张");
        output.getUnfavorableToPartyB().add("现有材料不足以完全排除 PartyB 的责任或抗辩风险");
        output.setFallback(true);
        output.setFailureReason(ex == null ? "法官输出不符合双向不利点要求" : ex.getMessage());
        return output;
    }
}
