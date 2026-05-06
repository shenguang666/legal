package com.legal.court.agent;

import com.legal.common.AppException;
import com.legal.court.dto.CourtEvidenceAllowedRefs;
import com.legal.court.dto.CourtRoleAgentRequest;
import com.legal.enums.CourtArgumentSpeaker;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 智能小法庭角色系统提示词模板服务。
 */
@Component
public class CourtRolePromptTemplateService {

    private final Map<CourtArgumentSpeaker, String> templates = new EnumMap<>(CourtArgumentSpeaker.class);

    public CourtRolePromptTemplateService(ResourceLoader resourceLoader) {
        templates.put(CourtArgumentSpeaker.OPPONENT, load(resourceLoader, "classpath:prompts/court-opponent-agent-system.txt"));
        templates.put(CourtArgumentSpeaker.USER_ADVISOR, load(resourceLoader, "classpath:prompts/court-user-advisor-agent-system.txt"));
        templates.put(CourtArgumentSpeaker.JUDGE, load(resourceLoader, "classpath:prompts/court-judge-agent-system.txt"));
    }

    /**
     * 按角色渲染系统提示词模板。
     */
    public String render(CourtArgumentSpeaker speaker, CourtRoleAgentRequest request) {
        String template = templates.get(speaker);
        if (!StringUtils.hasText(template)) {
            throw AppException.badRequest("智能小法庭角色提示词模板不存在");
        }
        return template
                .replace("{{stage}}", safe(request == null || request.getStage() == null ? null : request.getStage().getCode()))
                .replace("{{caseSummary}}", safe(request == null ? null : request.getCaseSummary()))
                .replace("{{instruction}}", safe(request == null ? null : request.getInstruction()))
                .replace("{{hearingHistory}}", history(request == null ? null : request.getHearingHistory()))
                .replace("{{evidenceContext}}", safe(request == null ? null : request.getEvidenceContext()))
                .replace("{{allowedRefs}}", whitelist(request == null ? null : request.getAllowedRefs()));
    }

    private String load(ResourceLoader resourceLoader, String location) {
        Resource resource = resourceLoader.getResource(location);
        try {
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("无法加载智能小法庭角色提示词模板: " + location, ex);
        }
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
        String joined = history.stream().filter(StringUtils::hasText).collect(Collectors.joining("\n"));
        return StringUtils.hasText(joined) ? joined : "无";
    }

    private String safe(String text) {
        return StringUtils.hasText(text) ? text : "无";
    }
}
