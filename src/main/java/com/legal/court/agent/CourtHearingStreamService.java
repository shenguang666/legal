package com.legal.court.agent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legal.common.AppException;
import com.legal.config.SmartCourtProperties;
import com.legal.court.dto.CourtAgentResponse;
import com.legal.court.dto.CourtAgentValidationResult;
import com.legal.court.dto.CourtEvidenceAllowedRefs;
import com.legal.court.dto.CourtEvidenceContext;
import com.legal.court.dto.CourtHearingRecordDto;
import com.legal.court.dto.CourtHearingStreamRequest;
import com.legal.court.dto.CourtHearingStreamEvent;
import com.legal.court.dto.CourtJudgeOutput;
import com.legal.court.dto.CourtJudgeValidationResult;
import com.legal.court.dto.CourtRoleAgentRequest;
import com.legal.court.dto.CourtRoleAgentResult;
import com.legal.court.entity.CourtArgumentEntity;
import com.legal.court.entity.CourtCaseEntity;
import com.legal.court.entity.CourtHearingMessageEntity;
import com.legal.court.entity.CourtHearingRoundEntity;
import com.legal.court.graph.CourtGraphEventPayload;
import com.legal.court.graph.CourtGraphEventPublisher;
import com.legal.court.mapper.CourtArgumentMapper;
import com.legal.court.mapper.CourtCaseMapper;
import com.legal.court.mapper.CourtHearingMessageMapper;
import com.legal.court.mapper.CourtHearingRoundMapper;
import com.legal.court.service.CourtCaseService;
import com.legal.court.service.CourtEvidenceContextService;
import com.legal.court.service.CourtEvidenceService;
import com.legal.court.service.CourtSuggestionService;
import com.legal.court.service.CourtTokenUsageService;
import com.legal.enums.CourtArgumentSpeaker;
import com.legal.enums.CourtArgumentStance;
import com.legal.enums.CourtCaseRole;
import com.legal.enums.CourtGraphEventType;
import com.legal.enums.CourtHearingStage;
import com.legal.enums.CourtHearingState;
import com.legal.enums.CourtPartyRole;
import com.legal.enums.CourtUserSide;
import com.legal.security.AuthPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * 智能小法庭流式庭审编排服务。
 */
@Slf4j
@Service
public class CourtHearingStreamService {

    private static final String BUSY_MESSAGE = "服务繁忙，请稍后重试";

    private final SmartCourtProperties properties;
    private final CourtCaseService courtCaseService;
    private final CourtEvidenceService courtEvidenceService;
    private final CourtEvidenceContextService courtEvidenceContextService;
    private final CourtRoleAgentService courtRoleAgentService;
    private final CourtAgentValidationService courtAgentValidationService;
    private final CourtTokenUsageService courtTokenUsageService;
    private final CourtGraphEventPublisher graphEventPublisher;
    private final CourtSuggestionService courtSuggestionService;
    private final CourtSseEmitterSupport emitterSupport;
    private final CourtHearingRoundMapper courtHearingRoundMapper;
    private final CourtHearingMessageMapper courtHearingMessageMapper;
    private final CourtArgumentMapper courtArgumentMapper;
    private final CourtCaseMapper courtCaseMapper;
    private final ObjectMapper objectMapper;

    public CourtHearingStreamService(SmartCourtProperties properties,
                                     CourtCaseService courtCaseService,
                                     CourtEvidenceService courtEvidenceService,
                                     CourtEvidenceContextService courtEvidenceContextService,
                                     CourtRoleAgentService courtRoleAgentService,
                                     CourtAgentValidationService courtAgentValidationService,
                                     CourtTokenUsageService courtTokenUsageService,
                                     CourtGraphEventPublisher graphEventPublisher,
                                     CourtSuggestionService courtSuggestionService,
                                     CourtSseEmitterSupport emitterSupport,
                                     CourtHearingRoundMapper courtHearingRoundMapper,
                                     CourtHearingMessageMapper courtHearingMessageMapper,
                                     CourtArgumentMapper courtArgumentMapper,
                                     CourtCaseMapper courtCaseMapper,
                                     ObjectMapper objectMapper) {
        this.properties = properties;
        this.courtCaseService = courtCaseService;
        this.courtEvidenceService = courtEvidenceService;
        this.courtEvidenceContextService = courtEvidenceContextService;
        this.courtRoleAgentService = courtRoleAgentService;
        this.courtAgentValidationService = courtAgentValidationService;
        this.courtTokenUsageService = courtTokenUsageService;
        this.graphEventPublisher = graphEventPublisher;
        this.courtSuggestionService = courtSuggestionService;
        this.emitterSupport = emitterSupport;
        this.courtHearingRoundMapper = courtHearingRoundMapper;
        this.courtHearingMessageMapper = courtHearingMessageMapper;
        this.courtArgumentMapper = courtArgumentMapper;
        this.courtCaseMapper = courtCaseMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 创建 SSE emitter，并异步执行完整一轮多角色庭审。
     */
    public SseEmitter startHearingStream(AuthPrincipal principal, Long caseId, String traceId) {
        CourtHearingStreamRequest request = new CourtHearingStreamRequest();
        request.setAdvisorAutoSpeak(true);
        return startHearingStream(principal, caseId, request, traceId);
    }

    /**
     * 创建 SSE emitter，并异步执行完整一轮多角色庭审。
     */
    public SseEmitter startHearingStream(AuthPrincipal principal, Long caseId, CourtHearingStreamRequest request, String traceId) {
        if (!properties.getStream().isEnabled()) {
            throw AppException.badRequest(BUSY_MESSAGE);
        }
        CourtHearingStreamRequest safeRequest = request == null ? new CourtHearingStreamRequest() : request;
        validateUserSpeechRequest(safeRequest);
        AtomicBoolean streamClosed = new AtomicBoolean(false);
        SseEmitter emitter = emitterSupport.createEmitter(traceId, streamClosed);
        CompletableFuture.runAsync(() -> runStream(principal, caseId, safeRequest, traceId, emitter, streamClosed));
        return emitter;
    }

    private void validateUserSpeechRequest(CourtHearingStreamRequest request) {
        if (Boolean.TRUE.equals(request.getAdvisorAutoSpeak()) || Boolean.TRUE.equals(request.getUserNoStatement())) {
            return;
        }
        if (!StringUtils.hasText(request.getUserStatement())) {
            throw AppException.badRequest("用户发言不能为空");
        }
    }

    private void runStream(AuthPrincipal principal, Long caseId, CourtHearingStreamRequest streamRequest, String traceId, SseEmitter emitter, AtomicBoolean streamClosed) {
        MDC.put("traceId", traceId);
        CourtHearingRoundEntity round = null;
        try {
            CourtCaseEntity courtCase = courtCaseService.startHearing(principal, caseId);
            round = createRunningRound(courtCase);
            send(emitter, streamClosed, traceId, "round-start", CourtHearingStreamEvent.of("round-start", caseId, round.getRoundId(), round.getAttemptId(), CourtArgumentSpeaker.SYSTEM, "庭审开始"));
            CourtRoleAgentRequest request = buildRequest(courtCase, round);
            if (request.getUserSide() == CourtCaseRole.PLAINTIFF) {
                invokeUserSide(emitter, streamClosed, traceId, round, request, streamRequest);
                invokeCommonRole(emitter, streamClosed, traceId, round, request, CourtArgumentSpeaker.OPPONENT);
            } else {
                invokeCommonRole(emitter, streamClosed, traceId, round, request, CourtArgumentSpeaker.OPPONENT);
                invokeUserSide(emitter, streamClosed, traceId, round, request, streamRequest);
            }
            invokeJudge(emitter, streamClosed, traceId, round, request);
            courtHearingRoundMapper.markSucceeded(round.getTenantId(), round.getCaseId(), round.getRoundId());
            courtCaseMapper.increaseTotalRounds(round.getTenantId(), round.getCaseId());
            courtSuggestionService.refreshSuggestions(round.getTenantId(), round.getCaseId(), round.getRoundId());
            send(emitter, streamClosed, traceId, "round-complete", CourtHearingStreamEvent.of("round-complete", caseId, round.getRoundId(), round.getAttemptId(), CourtArgumentSpeaker.SYSTEM, "庭审完成"));
        } catch (Exception ex) {
            Long roundId = round == null ? null : round.getRoundId();
            Integer attemptId = round == null ? null : round.getAttemptId();
            log.warn("court hearing stream failed tenantId={} caseId={} roundId={} error={}", principal.tenantId(), caseId, roundId, ex.getMessage());
            if (round != null) {
                courtHearingRoundMapper.markFailed(round.getTenantId(), round.getCaseId(), round.getRoundId(), truncate(ex.getMessage()));
            }
            send(emitter, streamClosed, traceId, "error", CourtHearingStreamEvent.of("error", caseId, roundId, attemptId, CourtArgumentSpeaker.SYSTEM, BUSY_MESSAGE));
        } finally {
            emitterSupport.safeComplete(emitter, streamClosed);
            MDC.remove("traceId");
        }
    }

    /**
     * 查询案件庭审历史记录。
     */
    public List<CourtHearingRecordDto> listRecords(AuthPrincipal principal, Long caseId) {
        CourtCaseEntity courtCase = courtCaseService.requireCase(principal, caseId);
        List<CourtHearingRoundEntity> rounds = courtHearingRoundMapper.selectList(new LambdaQueryWrapper<CourtHearingRoundEntity>()
                .eq(CourtHearingRoundEntity::getTenantId, courtCase.getTenantId())
                .eq(CourtHearingRoundEntity::getCaseId, courtCase.getCaseId())
                .orderByDesc(CourtHearingRoundEntity::getRoundNo));
        List<CourtHearingRecordDto> result = new ArrayList<>();
        for (CourtHearingRoundEntity round : rounds) {
            CourtHearingRecordDto dto = new CourtHearingRecordDto();
            dto.setRoundId(round.getRoundId());
            dto.setRoundNo(round.getRoundNo());
            dto.setStage(round.getStage());
            dto.setState(round.getState());
            dto.setStartedAt(round.getStartedAt());
            dto.setEndedAt(round.getEndedAt());
            dto.setMessages(loadRoundMessages(round));
            result.add(dto);
        }
        return result;
    }

    @Transactional
    CourtHearingRoundEntity createRunningRound(CourtCaseEntity courtCase) {
        rejectRunningRound(courtCase.getTenantId(), courtCase.getCaseId());
        int roundNo = (courtCase.getTotalRounds() == null ? 0 : courtCase.getTotalRounds()) + 1;
        if (roundNo > properties.getMaxRounds()) {
            throw AppException.badRequest("庭审轮次已达上限");
        }
        CourtHearingRoundEntity round = new CourtHearingRoundEntity();
        round.setTenantId(courtCase.getTenantId());
        round.setCaseId(courtCase.getCaseId());
        round.setRoundNo(roundNo);
        round.setStage(resolveStage());
        round.setState(CourtHearingState.RUNNING);
        round.setAttemptId(1);
        round.setLockVersion(1);
        round.setTotalTokens(0);
        round.setStartedAt(LocalDateTime.now());
        round.setCreatedAt(LocalDateTime.now());
        round.setUpdatedAt(LocalDateTime.now());
        courtHearingRoundMapper.insert(round);
        return round;
    }

    private void rejectRunningRound(Long tenantId, Long caseId) {
        CourtHearingRoundEntity running = courtHearingRoundMapper.selectOne(new LambdaQueryWrapper<CourtHearingRoundEntity>()
                .eq(CourtHearingRoundEntity::getTenantId, tenantId)
                .eq(CourtHearingRoundEntity::getCaseId, caseId)
                .eq(CourtHearingRoundEntity::getState, CourtHearingState.RUNNING)
                .last("limit 1"));
        if (running != null) {
            throw AppException.badRequest("庭审进行中");
        }
    }

    private CourtRoleAgentRequest buildRequest(CourtCaseEntity courtCase, CourtHearingRoundEntity round) {
        CourtEvidenceAllowedRefs allowedRefs = courtEvidenceService.allowedRefsForRound(courtCase.getCaseId(), courtCase.getTenantId(), round.getRoundId());
        CourtEvidenceContext evidenceContext = courtEvidenceContextService.retrieveForCourt(courtCase.getTenantId(), courtCase.getCaseSummary(), properties.getRetrieval().getTopK());
        CourtRoleAgentRequest request = new CourtRoleAgentRequest();
        request.setTenantId(courtCase.getTenantId());
        request.setCaseId(courtCase.getCaseId());
        request.setRoundId(round.getRoundId());
        request.setStage(round.getStage());
        request.setUserSide(toCaseRole(courtCase.getUserSide()));
        request.setInstruction("请完成本轮智能小法庭开庭发言");
        request.setCaseSummary(courtCase.getCaseSummary());
        request.setEvidenceContext(formatEvidenceContext(evidenceContext));
        request.setAllowedRefs(allowedRefs);
        request.setHearingHistory(loadHearingHistory(courtCase.getTenantId(), courtCase.getCaseId()));
        return request;
    }

    private void invokeUserSide(SseEmitter emitter,
                                AtomicBoolean streamClosed,
                                String traceId,
                                CourtHearingRoundEntity round,
                                CourtRoleAgentRequest request,
                                CourtHearingStreamRequest streamRequest) throws JsonProcessingException {
        if (Boolean.TRUE.equals(streamRequest.getAdvisorAutoSpeak())) {
            invokeCommonRole(emitter, streamClosed, traceId, round, request, CourtArgumentSpeaker.USER_ADVISOR);
            return;
        }
        recordUserStatement(emitter, streamClosed, traceId, round, request.getUserSide(), streamRequest.getUserStatement(), streamRequest.getUserNoStatement());
    }

    private void invokeCommonRole(SseEmitter emitter,
                                  AtomicBoolean streamClosed,
                                  String traceId,
                                  CourtHearingRoundEntity round,
                                  CourtRoleAgentRequest request,
                                  CourtArgumentSpeaker speaker) throws JsonProcessingException {
        send(emitter, streamClosed, traceId, "role-start", CourtHearingStreamEvent.of("role-start", round.getCaseId(), round.getRoundId(), round.getAttemptId(), speaker, roleName(speaker) + "开始发言"));
        CourtRoleAgentResult result = speaker == CourtArgumentSpeaker.OPPONENT
                ? courtRoleAgentService.invokeOpponent(request)
                : courtRoleAgentService.invokeUserAdvisor(request);
        CourtHearingMessageEntity message = courtTokenUsageService.recordAgentMessage(round.getTenantId(), round.getCaseId(), round.getRoundId(), round.getAttemptId(), result);
        CourtAgentResponse response = objectMapper.readValue(result.getRawText(), CourtAgentResponse.class);
        CourtAgentValidationResult validationResult = courtAgentValidationService.validateWithRetry(() -> response, request.getAllowedRefs());
        CourtArgumentEntity argument = saveCommonArgument(round, message, speaker, request.getUserSide(), validationResult);
        publishArgumentNode(round, argument);
        CourtHearingStreamEvent event = CourtHearingStreamEvent.of("role-complete", round.getCaseId(), round.getRoundId(), round.getAttemptId(), speaker, validationResult.getContent());
        event.setModelName(result.getModelName());
        event.setTokenUsage(result.getTokenUsage());
        send(emitter, streamClosed, traceId, "role-complete", event);
    }

    private void invokeJudge(SseEmitter emitter,
                             AtomicBoolean streamClosed,
                             String traceId,
                             CourtHearingRoundEntity round,
                             CourtRoleAgentRequest request) throws JsonProcessingException {
        send(emitter, streamClosed, traceId, "role-start", CourtHearingStreamEvent.of("role-start", round.getCaseId(), round.getRoundId(), round.getAttemptId(), CourtArgumentSpeaker.JUDGE, "法官开始归纳"));
        CourtJudgeValidationResult result = courtRoleAgentService.invokeJudgeWithValidation(request);
        CourtRoleAgentResult messageResult = new CourtRoleAgentResult();
        messageResult.setSpeaker(CourtArgumentSpeaker.JUDGE);
        messageResult.setModelName(result.getModelName());
        messageResult.setRawText(result.getRawText());
        messageResult.setTokenUsage(result.getTokenUsage());
        CourtHearingMessageEntity message = courtTokenUsageService.recordAgentMessage(round.getTenantId(), round.getCaseId(), round.getRoundId(), round.getAttemptId(), messageResult);
        CourtArgumentEntity argument = saveJudgeArgument(round, message, result);
        publishArgumentNode(round, argument);
        CourtHearingStreamEvent event = CourtHearingStreamEvent.of("role-complete", round.getCaseId(), round.getRoundId(), round.getAttemptId(), CourtArgumentSpeaker.JUDGE, renderJudgeSummary(result.getOutput()));
        event.setModelName(result.getModelName());
        event.setTokenUsage(result.getTokenUsage());
        send(emitter, streamClosed, traceId, "role-complete", event);
    }

    private CourtArgumentEntity saveCommonArgument(CourtHearingRoundEntity round,
                                                   CourtHearingMessageEntity message,
                                                   CourtArgumentSpeaker speaker,
                                                   CourtCaseRole userSide,
                                                   CourtAgentValidationResult validationResult) throws JsonProcessingException {
        CourtArgumentEntity argument = new CourtArgumentEntity();
        argument.setTenantId(round.getTenantId());
        argument.setCaseId(round.getCaseId());
        argument.setRoundId(round.getRoundId());
        argument.setMessageId(message.getMessageId());
        argument.setSpeakerRole(speaker);
        argument.setSpeakerParty(speakerParty(speaker, userSide));
        courtAgentValidationService.applyToArgument(argument, validationResult);
        argument.setEvidenceRefsJson(objectMapper.writeValueAsString(validationResult.getValidRefs()));
        argument.setChallengedArgumentIdsJson("[]");
        argument.setCreatedAt(LocalDateTime.now());
        courtArgumentMapper.insert(argument);
        return argument;
    }

    private CourtArgumentEntity saveJudgeArgument(CourtHearingRoundEntity round,
                                                  CourtHearingMessageEntity message,
                                                  CourtJudgeValidationResult result) throws JsonProcessingException {
        CourtArgumentEntity argument = new CourtArgumentEntity();
        argument.setTenantId(round.getTenantId());
        argument.setCaseId(round.getCaseId());
        argument.setRoundId(round.getRoundId());
        argument.setMessageId(message.getMessageId());
        argument.setSpeakerRole(CourtArgumentSpeaker.JUDGE);
        argument.setStance(CourtArgumentStance.NEUTRAL);
        argument.setContent(renderJudgeSummary(result.getOutput()));
        argument.setRationale(result.isValid() ? "法官输出通过双向不利点校验" : result.getFailureReason());
        argument.setEvidenceRefsJson("[]");
        argument.setChallengedArgumentIdsJson("[]");
        argument.setEvidenceVerifiedCount(0);
        argument.setEvidenceDroppedCount(0);
        argument.setCreatedAt(LocalDateTime.now());
        courtArgumentMapper.insert(argument);
        return argument;
    }

    private void recordUserStatement(SseEmitter emitter,
                                     AtomicBoolean streamClosed,
                                     String traceId,
                                     CourtHearingRoundEntity round,
                                     CourtCaseRole userSide,
                                     String userStatement,
                                     Boolean userNoStatement) {
        send(emitter, streamClosed, traceId, "role-start", CourtHearingStreamEvent.of("role-start", round.getCaseId(), round.getRoundId(), round.getAttemptId(), CourtArgumentSpeaker.USER, "用户开始发言"));
        String content = Boolean.TRUE.equals(userNoStatement) ? "用户本轮暂不补充发言。" : userStatement.trim();
        CourtHearingMessageEntity message = courtTokenUsageService.recordUserMessage(round.getTenantId(), round.getCaseId(), round.getRoundId(), round.getAttemptId(), speakerParty(CourtArgumentSpeaker.USER_ADVISOR, userSide), content);
        CourtArgumentEntity argument = saveUserArgument(round, message, userSide, content);
        publishArgumentNode(round, argument);
        send(emitter, streamClosed, traceId, "role-complete", CourtHearingStreamEvent.of("role-complete", round.getCaseId(), round.getRoundId(), round.getAttemptId(), CourtArgumentSpeaker.USER, content));
    }

    private CourtArgumentEntity saveUserArgument(CourtHearingRoundEntity round,
                                                 CourtHearingMessageEntity message,
                                                 CourtCaseRole userSide,
                                                 String content) {
        CourtArgumentEntity argument = new CourtArgumentEntity();
        argument.setTenantId(round.getTenantId());
        argument.setCaseId(round.getCaseId());
        argument.setRoundId(round.getRoundId());
        argument.setMessageId(message.getMessageId());
        argument.setSpeakerRole(CourtArgumentSpeaker.USER);
        argument.setSpeakerParty(speakerParty(CourtArgumentSpeaker.USER_ADVISOR, userSide));
        argument.setStance(CourtArgumentStance.SUPPORT);
        argument.setContent(content);
        argument.setRationale("用户庭审发言");
        argument.setEvidenceRefsJson("[]");
        argument.setChallengedArgumentIdsJson("[]");
        argument.setEvidenceVerifiedCount(0);
        argument.setEvidenceDroppedCount(0);
        argument.setCreatedAt(LocalDateTime.now());
        courtArgumentMapper.insert(argument);
        return argument;
    }

    private void publishArgumentNode(CourtHearingRoundEntity round, CourtArgumentEntity argument) {
        CourtGraphEventPayload payload = new CourtGraphEventPayload();
        payload.setLabel("Argument");
        payload.setBusinessId("argument-" + argument.getArgumentId());
        payload.getProperties().put("speakerRole", argument.getSpeakerRole() == null ? null : argument.getSpeakerRole().getCode());
        payload.getProperties().put("speakerParty", argument.getSpeakerParty() == null ? null : argument.getSpeakerParty().getCode());
        payload.getProperties().put("stance", argument.getStance() == null ? null : argument.getStance().getCode());
        payload.getProperties().put("content", argument.getContent());
        graphEventPublisher.publish(round.getTenantId(), round.getCaseId(), round.getRoundId(), CourtGraphEventType.UPSERT_NODE, payload);
        publishRelation(round, "case-" + round.getCaseId(), "Case", payload.getBusinessId(), "Argument", caseArgumentRelation(argument));
        Long previousArgumentId = previousArgumentId(round, argument.getArgumentId());
        if (previousArgumentId != null) {
            CourtArgumentEntity previous = courtArgumentMapper.selectById(previousArgumentId);
            publishRelation(round, "argument-" + previousArgumentId, "Argument", payload.getBusinessId(), "Argument", argumentRelation(previous, argument));
        }
    }

    private String caseArgumentRelation(CourtArgumentEntity argument) {
        if (argument.getSpeakerRole() == CourtArgumentSpeaker.JUDGE) {
            return "SUMMARIZES_ARGUMENT";
        }
        if (argument.getSpeakerRole() == CourtArgumentSpeaker.OPPONENT) {
            return "OPPOSES_ARGUMENT";
        }
        if (argument.getSpeakerRole() == CourtArgumentSpeaker.USER_ADVISOR) {
            return "ADVISES_ARGUMENT";
        }
        return "PRESENTS_ARGUMENT";
    }

    private String argumentRelation(CourtArgumentEntity previous, CourtArgumentEntity current) {
        if (current.getSpeakerRole() == CourtArgumentSpeaker.JUDGE) {
            return "SUMMARIZES_ARGUMENT";
        }
        if (previous != null && previous.getSpeakerParty() != null && current.getSpeakerParty() != null
                && previous.getSpeakerParty() != current.getSpeakerParty()) {
            return current.getStance() == CourtArgumentStance.REBUT ? "REBUTS_ARGUMENT" : "CHALLENGES_ARGUMENT";
        }
        return "SUPPORTS_ARGUMENT";
    }

    private void publishRelation(CourtHearingRoundEntity round,
                                 String fromBusinessId,
                                 String fromLabel,
                                 String toBusinessId,
                                 String toLabel,
                                 String relationType) {
        CourtGraphEventPayload payload = new CourtGraphEventPayload();
        payload.setBusinessId(fromBusinessId + "-" + relationType + "-" + toBusinessId);
        payload.setFromBusinessId(fromBusinessId);
        payload.setFromLabel(fromLabel);
        payload.setToBusinessId(toBusinessId);
        payload.setToLabel(toLabel);
        payload.setRelationType(relationType);
        payload.getProperties().put("label", relationType);
        payload.getProperties().put("sourceBusinessId", fromBusinessId);
        payload.getProperties().put("targetBusinessId", toBusinessId);
        graphEventPublisher.publish(round.getTenantId(), round.getCaseId(), round.getRoundId(), CourtGraphEventType.UPSERT_RELATION, payload);
    }

    private Long previousArgumentId(CourtHearingRoundEntity round, Long currentArgumentId) {
        CourtArgumentEntity previous = courtArgumentMapper.selectOne(new LambdaQueryWrapper<CourtArgumentEntity>()
                .eq(CourtArgumentEntity::getTenantId, round.getTenantId())
                .eq(CourtArgumentEntity::getCaseId, round.getCaseId())
                .eq(CourtArgumentEntity::getRoundId, round.getRoundId())
                .lt(CourtArgumentEntity::getArgumentId, currentArgumentId)
                .orderByDesc(CourtArgumentEntity::getArgumentId)
                .last("limit 1"));
        return previous == null ? null : previous.getArgumentId();
    }

    private List<CourtHearingRecordDto.MessageDto> loadRoundMessages(CourtHearingRoundEntity round) {
        List<CourtArgumentEntity> arguments = courtArgumentMapper.selectList(new LambdaQueryWrapper<CourtArgumentEntity>()
                .eq(CourtArgumentEntity::getTenantId, round.getTenantId())
                .eq(CourtArgumentEntity::getCaseId, round.getCaseId())
                .eq(CourtArgumentEntity::getRoundId, round.getRoundId())
                .orderByAsc(CourtArgumentEntity::getCreatedAt));
        List<CourtHearingRecordDto.MessageDto> result = new ArrayList<>();
        for (CourtArgumentEntity argument : arguments) {
            CourtHearingRecordDto.MessageDto dto = new CourtHearingRecordDto.MessageDto();
            CourtHearingMessageEntity message = argument.getMessageId() == null ? null : courtHearingMessageMapper.selectById(argument.getMessageId());
            dto.setMessageId(argument.getMessageId());
            dto.setArgumentId(argument.getArgumentId());
            dto.setSpeaker(argument.getSpeakerRole());
            dto.setSpeakerParty(argument.getSpeakerParty());
            dto.setStance(argument.getStance());
            dto.setContent(argument.getContent());
            dto.setRationale(argument.getRationale());
            dto.setEvidenceRefsJson(argument.getEvidenceRefsJson());
            dto.setTokenUsage(message == null || message.getTokenOutput() == null ? 0 : message.getTokenOutput());
            dto.setCreatedAt(argument.getCreatedAt());
            result.add(dto);
        }
        return result;
    }

    private List<String> loadHearingHistory(Long tenantId, Long caseId) {
        List<CourtArgumentEntity> arguments = courtArgumentMapper.selectList(new LambdaQueryWrapper<CourtArgumentEntity>()
                .eq(CourtArgumentEntity::getTenantId, tenantId)
                .eq(CourtArgumentEntity::getCaseId, caseId)
                .orderByDesc(CourtArgumentEntity::getCreatedAt)
                .last("limit 12"));
        List<String> result = new ArrayList<>();
        for (CourtArgumentEntity argument : arguments) {
            String speaker = argument.getSpeakerRole() == null ? "UNKNOWN" : argument.getSpeakerRole().getCode();
            result.add(speaker + ": " + argument.getContent());
        }
        return result;
    }

    private String formatEvidenceContext(CourtEvidenceContext context) {
        if (context == null || context.getItems().isEmpty()) {
            return "无";
        }
        return context.getItems().stream()
                .map(item -> "documentId=" + item.getDocumentId()
                        + ", parentChunkId=" + item.getParentChunkId()
                        + ", childChunkId=" + item.getHitChildChunkId()
                        + "\n" + trim(item.getContent()))
                .collect(Collectors.joining("\n---\n"));
    }

    private String renderJudgeSummary(CourtJudgeOutput output) {
        if (output == null) {
            return "证据不足，无法形成倾向性意见";
        }
        return "争议焦点：" + String.join("；", output.getFocusIssues())
                + "\nPartyA 不利点：" + String.join("；", output.getUnfavorableToPartyA())
                + "\nPartyB 不利点：" + String.join("；", output.getUnfavorableToPartyB())
                + "\n开放问题：" + String.join("；", output.getOpenQuestions());
    }

    private CourtHearingStage resolveStage() {
        String configured = properties.getHearing().getInitialStage();
        if (StringUtils.hasText(configured)) {
            try {
                return CourtHearingStage.valueOf(configured);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return CourtHearingStage.OPENING_PLAINTIFF;
    }

    private CourtPartyRole speakerParty(CourtArgumentSpeaker speaker, CourtCaseRole userSide) {
        if (speaker == CourtArgumentSpeaker.JUDGE || speaker == CourtArgumentSpeaker.SYSTEM) {
            return null;
        }
        if (speaker == CourtArgumentSpeaker.USER_ADVISOR) {
            return userSide == CourtCaseRole.DEFENDANT ? CourtPartyRole.DEFENDANT : CourtPartyRole.PLAINTIFF;
        }
        return userSide == CourtCaseRole.DEFENDANT ? CourtPartyRole.PLAINTIFF : CourtPartyRole.DEFENDANT;
    }

    private CourtCaseRole toCaseRole(CourtUserSide userSide) {
        return userSide == CourtUserSide.DEFENDANT ? CourtCaseRole.DEFENDANT : CourtCaseRole.PLAINTIFF;
    }

    private String roleName(CourtArgumentSpeaker speaker) {
        if (speaker == CourtArgumentSpeaker.OPPONENT) {
            return "对方代理人";
        }
        if (speaker == CourtArgumentSpeaker.USER_ADVISOR) {
            return "用户辅助律师";
        }
        if (speaker == CourtArgumentSpeaker.JUDGE) {
            return "法官";
        }
        return "系统";
    }

    private void send(SseEmitter emitter, AtomicBoolean streamClosed, String traceId, String eventName, CourtHearingStreamEvent event) {
        emitterSupport.safeSend(emitter, streamClosed, traceId, eventName, event);
    }

    private String trim(String text) {
        if (!StringUtils.hasText(text)) {
            return "无";
        }
        int max = Math.max(200, properties.getRetrieval().getMaxContextCharsPerEvidence());
        return text.length() <= max ? text : text.substring(0, max);
    }

    private String truncate(String message) {
        if (!StringUtils.hasText(message)) {
            return BUSY_MESSAGE;
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
