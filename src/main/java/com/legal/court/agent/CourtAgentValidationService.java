package com.legal.court.agent;

import com.legal.common.AppException;
import com.legal.config.SmartCourtProperties;
import com.legal.court.dto.CourtAgentResponse;
import com.legal.court.dto.CourtAgentValidationResult;
import com.legal.court.dto.CourtEvidenceAllowedRefs;
import com.legal.court.entity.CourtArgumentEntity;
import com.legal.enums.CourtArgumentStance;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

/**
 * 智能小法庭 AI 输出校验编排服务。
 */
@Service
public class CourtAgentValidationService {

    private final CourtAgentResponseValidator validator;
    private final SmartCourtProperties properties;
    private final Counter evidenceHallucinationCounter;

    public CourtAgentValidationService(CourtAgentResponseValidator validator,
                                       SmartCourtProperties properties,
                                       MeterRegistry meterRegistry) {
        this.validator = validator;
        this.properties = properties;
        this.evidenceHallucinationCounter = Counter.builder("court.agent.evidence_hallucination")
                .description("智能小法庭 AI 输出非法证据引用次数")
                .register(meterRegistry);
    }

    /**
     * 执行带 schema 失败重试的 AI 输出校验。
     */
    public CourtAgentValidationResult validateWithRetry(Supplier<CourtAgentResponse> responseSupplier,
                                                        CourtEvidenceAllowedRefs allowedRefs) {
        int maxRetries = Math.max(0, properties.getEvidenceRefRetry());
        AppException lastSchemaException = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            CourtAgentResponse response = responseSupplier.get();
            try {
                CourtAgentValidationResult result = validator.validate(response, allowedRefs);
                recordMetricIfNeeded(result);
                return result;
            } catch (AppException ex) {
                lastSchemaException = ex;
                if (attempt >= maxRetries) {
                    break;
                }
            }
        }
        CourtAgentValidationResult fallback = fallbackPendingProof(lastSchemaException);
        recordMetricIfNeeded(fallback);
        return fallback;
    }

    /**
     * 将校验结果写入庭审观点实体。
     */
    public void applyToArgument(CourtArgumentEntity argument, CourtAgentValidationResult result) {
        argument.setContent(result.getContent());
        argument.setRationale(result.getRationale());
        argument.setStance(result.getStance());
        argument.setEvidenceVerifiedCount(result.getEvidenceVerifiedCount());
        argument.setEvidenceDroppedCount(result.getEvidenceDroppedCount());
    }

    private CourtAgentValidationResult fallbackPendingProof(AppException ex) {
        CourtAgentValidationResult result = new CourtAgentValidationResult();
        result.setContent("待证明：AI 输出未通过证据引用结构校验，请补充或重新生成。 ");
        result.setStance(CourtArgumentStance.PENDING_PROOF);
        result.setRationale("证据引用 schema 校验失败");
        result.setEvidenceVerifiedCount(0);
        result.setEvidenceDroppedCount(1);
        result.setDegradedToPendingProof(true);
        result.setFailureReason(ex == null ? "AI 输出结构不符合证据引用 schema" : ex.getMessage());
        return result;
    }

    private void recordMetricIfNeeded(CourtAgentValidationResult result) {
        if (result.getEvidenceDroppedCount() > 0 || result.isDegradedToPendingProof()) {
            evidenceHallucinationCounter.increment(Math.max(1, result.getEvidenceDroppedCount()));
        }
    }
}
