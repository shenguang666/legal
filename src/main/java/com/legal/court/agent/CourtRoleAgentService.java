package com.legal.court.agent;

import com.legal.common.AppException;
import com.legal.config.OpenAiChatModelProperties;
import com.legal.court.dto.CourtJudgeOutput;
import com.legal.court.dto.CourtJudgeValidationResult;
import com.legal.court.dto.CourtEvidenceAllowedRefs;
import com.legal.court.dto.CourtRoleAgentRequest;
import com.legal.court.dto.CourtRoleAgentResult;
import com.legal.enums.CourtArgumentSpeaker;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 智能小法庭多角色 Agent 服务。
 */
@Service
public class CourtRoleAgentService {

    private final ChatModel chatModel;
    private final OpenAiChatModelProperties modelProperties;
    private final CourtJudgePerspectiveAnonymizer judgePerspectiveAnonymizer;
    private final CourtJudgeOutputValidator judgeOutputValidator;

    public CourtRoleAgentService(@Nullable ChatModel chatModel,
                                 OpenAiChatModelProperties modelProperties,
                                 CourtJudgePerspectiveAnonymizer judgePerspectiveAnonymizer,
                                 CourtJudgeOutputValidator judgeOutputValidator) {
        this.chatModel = chatModel;
        this.modelProperties = modelProperties;
        this.judgePerspectiveAnonymizer = judgePerspectiveAnonymizer;
        this.judgeOutputValidator = judgeOutputValidator;
    }

    /**
     * 调用 AI 法官 Agent。
     */
    public CourtRoleAgentResult invokeJudge(CourtRoleAgentRequest request) {
        return invoke(CourtArgumentSpeaker.JUDGE, judgeSystemPrompt(), buildJudgeUserPrompt(request));
    }

    /**
     * 调用 AI 法官 Agent，并强制校验双向不利点。
     */
    public CourtJudgeValidationResult invokeJudgeWithValidation(CourtRoleAgentRequest request) {
        AppException lastException = null;
        CourtRoleAgentResult lastResult = null;
        for (int retry = 0; retry <= 2; retry++) {
            lastResult = invokeJudge(request);
            try {
                CourtJudgeOutput output = judgeOutputValidator.validate(lastResult.getRawText());
                CourtJudgeValidationResult result = new CourtJudgeValidationResult();
                result.setOutput(output);
                result.setRawText(lastResult.getRawText());
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
        fallback.setValid(false);
        fallback.setRetryCount(2);
        fallback.setFailureReason(lastException == null ? "法官输出不符合双向不利点要求" : lastException.getMessage());
        return fallback;
    }

    /**
     * 调用 AI 对方代理人 Agent。
     */
    public CourtRoleAgentResult invokeOpponent(CourtRoleAgentRequest request) {
        return invoke(CourtArgumentSpeaker.OPPONENT, opponentSystemPrompt(), buildCommonUserPrompt(request));
    }

    /**
     * 调用 AI 用户辅助律师 Agent。
     */
    public CourtRoleAgentResult invokeUserAdvisor(CourtRoleAgentRequest request) {
        return invoke(CourtArgumentSpeaker.USER_ADVISOR, userAdvisorSystemPrompt(), buildCommonUserPrompt(request));
    }

    private CourtRoleAgentResult invoke(CourtArgumentSpeaker speaker, String systemPrompt, String userPrompt) {
        if (chatModel == null) {
            throw AppException.badRequest("请先配置 LEGAL_LLM_API_KEY 后再使用智能小法庭 Agent");
        }
        ChatResponse response = chatModel.chat(List.of(SystemMessage.from(systemPrompt), UserMessage.from(userPrompt)));
        CourtRoleAgentResult result = new CourtRoleAgentResult();
        result.setSpeaker(speaker);
        result.setModelName(modelProperties.getModelName());
        result.setRawText(response.aiMessage() == null ? "" : response.aiMessage().text());
        result.setTokenUsage(response.tokenUsage() == null ? 0 : response.tokenUsage().totalTokenCount());
        return result;
    }

    private String judgeSystemPrompt() {
        return "你是中立的合同纠纷模拟法官。必须只输出 JSON，不得偏向任何一方，不得出现 user、用户、一方是用户等措辞。必须同时指出 PartyA 与 PartyB 的不利点。";
    }

    private String opponentSystemPrompt() {
        return "你是合同纠纷模拟庭审中的对方代理人。必须只输出 JSON。只能引用白名单中的 evidenceId、parentChunkId、childChunkId，不得编造合同条款、事实或证据编号。";
    }

    private String userAdvisorSystemPrompt() {
        return "你是用户一方的辅助律师。必须只输出 JSON。职责是帮助用户补强主张、提示证据缺口，但不得编造证据，不得引用白名单外的编号。";
    }

    private String buildJudgeUserPrompt(CourtRoleAgentRequest request) {
        return judgePerspectiveAnonymizer.anonymize(buildBasePrompt(request), request.getUserSide())
                + "\n输出 JSON schema：{\"focusIssues\":[],\"acceptedFacts\":[],\"rejectedFacts\":[],\"unfavorableToPartyA\":[],\"unfavorableToPartyB\":[],\"openQuestions\":[]}。";
    }

    private String buildCommonUserPrompt(CourtRoleAgentRequest request) {
        return buildBasePrompt(request)
                + "\n输出 JSON schema：{\"content\":\"...\",\"stance\":\"SUPPORT|REBUT|NEUTRAL|PENDING_PROOF\",\"rationale\":\"...\",\"evidenceRefs\":[{\"evidenceId\":1,\"parentChunkId\":2,\"childChunkId\":3,\"reason\":\"...\"}]}。";
    }

    private String buildBasePrompt(CourtRoleAgentRequest request) {
        return "庭审阶段：" + safe(request.getStage() == null ? null : request.getStage().getCode())
                + "\n案件摘要：" + safe(request.getCaseSummary())
                + "\n本轮指令：" + safe(request.getInstruction())
                + "\n历史庭审摘要：" + history(request.getHearingHistory())
                + "\n证据上下文：" + safe(request.getEvidenceContext())
                + "\n允许引用白名单：" + whitelist(request.getAllowedRefs());
    }

    private String whitelist(CourtEvidenceAllowedRefs refs) {
        if (refs == null) {
            return "evidenceIds=[], parentChunkIds=[], childChunkIds=[]";
        }
        return "evidenceIds=" + refs.getEvidenceIds()
                + ", parentChunkIds=" + refs.getParentChunkIds()
                + ", childChunkIds=" + refs.getChildChunkIds();
    }

    private String history(List<String> history) {
        if (history == null || history.isEmpty()) {
            return "无";
        }
        return history.stream().filter(StringUtils::hasText).collect(Collectors.joining("\n"));
    }

    private String safe(String text) {
        return StringUtils.hasText(text) ? text : "无";
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
