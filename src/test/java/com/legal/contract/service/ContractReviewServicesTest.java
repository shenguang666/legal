package com.legal.contract.service;

import com.legal.config.ElasticsearchProperties;
import com.legal.config.ContractReviewProperties;
import com.legal.contract.entity.ContractFieldDefinitionEntity;
import com.legal.contract.mapper.ContractRuleDefinitionMapper;
import com.legal.contract.entity.ContractRuleDefinitionEntity;
import com.legal.enums.ContractFieldStatus;
import com.legal.enums.ContractRiskSeverity;
import com.legal.enums.ContractRuleExecutionStatus;
import com.legal.enums.ContractRuleType;
import com.legal.knowledge.entity.KbChunkEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContractReviewServicesTest {

    @Test
    void extractionShouldProduceMissingAndRepeatableFields() {
        ContractReviewProperties properties = new ContractReviewProperties();
        ContractFieldDefinitionService fieldDefinitionService = mock(ContractFieldDefinitionService.class);
        when(fieldDefinitionService.listEnabledDefinitions(1L)).thenReturn(defaultFieldDefinitions());
        ContractFieldExtractionService service = new ContractFieldExtractionService(properties, fieldDefinitionService);
        KbChunkEntity chunk = new KbChunkEntity();
        chunk.setChunkOrder(1);
        chunk.setContent("甲方：上海甲公司\n乙方：杭州乙公司\n合同金额：10000元\n合同金额：12000元\n付款日期：2026年05月01日\n报销项目：交通报销\n报销项目：住宿报销\n违约责任：乙方承担违约责任");

        List<ExtractedContractField> fields = service.extract(1L, List.of(chunk));
        assertTrue(fields.stream().anyMatch(f -> "party_a".equals(f.getFieldCode()) && f.getStatus() == ContractFieldStatus.EXTRACTED));
        assertTrue(fields.stream().filter(f -> "contract_amount".equals(f.getFieldCode()) && f.getStatus() == ContractFieldStatus.EXTRACTED).count() >= 2);
        assertTrue(fields.stream().filter(f -> "reimbursement_item".equals(f.getFieldCode())).count() >= 2);
    }

    @Test
    void validatorsShouldDetectAmountConflictAndMissingRequiredField() {
        RequiredFieldRuleValidator requiredValidator = new RequiredFieldRuleValidator();
        AmountConsistencyRuleValidator amountValidator = new AmountConsistencyRuleValidator();

        ExtractedContractField partyA = field("party_a", "上海甲公司", ContractFieldStatus.EXTRACTED);
        ExtractedContractField amount1 = field("contract_amount", "10000.00", ContractFieldStatus.EXTRACTED);
        ExtractedContractField amount2 = field("contract_amount", "15000.00", ContractFieldStatus.EXTRACTED);

        ContractRuleDefinitionEntity requiredRule = new ContractRuleDefinitionEntity();
        requiredRule.setRuleCode("REQUIRED_PARTY_B");
        requiredRule.setRuleName("乙方必填");
        requiredRule.setRuleType(ContractRuleType.REQUIRED_FIELD);
        requiredRule.setFieldCode("party_b");
        requiredRule.setSeverity(ContractRiskSeverity.HIGH);

        ContractRuleDefinitionEntity amountRule = new ContractRuleDefinitionEntity();
        amountRule.setRuleCode("AMOUNT_CONSISTENCY");
        amountRule.setRuleName("金额一致性校验");
        amountRule.setRuleType(ContractRuleType.AMOUNT_CONSISTENCY);
        amountRule.setFieldCode("contract_amount");
        amountRule.setSeverity(ContractRiskSeverity.HIGH);

        ContractRuleEvaluation missing = requiredValidator.validate(requiredRule, List.of(partyA, amount1, amount2));
        ContractRuleEvaluation conflict = amountValidator.validate(amountRule, List.of(partyA, amount1, amount2));

        assertEquals(ContractRuleExecutionStatus.HIT, missing.getExecutionStatus());
        assertEquals(ContractRuleExecutionStatus.HIT, conflict.getExecutionStatus());
        assertTrue(conflict.getMessage().contains("不一致金额"));
    }

    @Test
    void sampleContractFlowShouldSurfaceMissingFieldsAndClauseConflict() {
        ContractReviewProperties properties = new ContractReviewProperties();
        ContractFieldDefinitionService fieldDefinitionService = mock(ContractFieldDefinitionService.class);
        when(fieldDefinitionService.listEnabledDefinitions(1L)).thenReturn(defaultFieldDefinitions());
        ContractFieldExtractionService extractionService = new ContractFieldExtractionService(properties, fieldDefinitionService);
        ContractRuleDefinitionMapper mapper = mock(ContractRuleDefinitionMapper.class);
        when(mapper.selectActiveRules(1L)).thenReturn(List.of(
                rule("REQUIRED_PARTY_B", ContractRuleType.REQUIRED_FIELD, "party_b", ContractRiskSeverity.HIGH, null),
                rule("AMOUNT_CONSISTENCY", ContractRuleType.AMOUNT_CONSISTENCY, "contract_amount", ContractRiskSeverity.HIGH, null),
                rule("LIABILITY_CONFLICT", ContractRuleType.LIABILITY_CONFLICT, "liability_clause", ContractRiskSeverity.HIGH, null)
        ));
        RiskRuleDocumentMatchService matchService = mock(RiskRuleDocumentMatchService.class);
        when(matchService.match(anyLong(), anyList(), anyList(), anyString())).thenReturn(List.of());
        ContractRuleValidationService validationService = new ContractRuleValidationService(mapper, List.of(
                new RequiredFieldRuleValidator(),
                new AmountConsistencyRuleValidator(),
                new LiabilityConflictRuleValidator()
        ), matchService, new ElasticsearchProperties());

        KbChunkEntity chunk = new KbChunkEntity();
        chunk.setChunkOrder(1);
        chunk.setContent("甲方：示例科技有限公司\n合同金额：10000元\n合同金额：18000元\n责任承担：乙方承担全部赔偿责任。\n免责条款：甲方不承担任何责任。\n报销项目：交通报销");

        List<ExtractedContractField> fields = extractionService.extract(1L, List.of(chunk));
        List<ContractRuleEvaluation> evaluations = validationService.evaluate(1L, List.of(chunk), fields);

        assertTrue(fields.stream().anyMatch(field -> "party_b".equals(field.getFieldCode()) && field.getStatus() == ContractFieldStatus.MISSING));
        assertTrue(evaluations.stream().anyMatch(item -> "REQUIRED_PARTY_B".equals(item.getRuleCode()) && item.getExecutionStatus() == ContractRuleExecutionStatus.HIT));
        assertTrue(evaluations.stream().anyMatch(item -> "AMOUNT_CONSISTENCY".equals(item.getRuleCode()) && item.getExecutionStatus() == ContractRuleExecutionStatus.HIT));
        assertTrue(evaluations.stream().anyMatch(item -> "LIABILITY_CONFLICT".equals(item.getRuleCode()) && item.getExecutionStatus() == ContractRuleExecutionStatus.HIT));
    }

    private List<ContractFieldDefinitionEntity> defaultFieldDefinitions() {
        return List.of(
                fieldDefinition("party_a", "甲方", "PARTY_PATTERN", "(?:甲方|采购方|发包方|委托方)\\s*[：:]\\s*([^\\n，。,；;]{2,40})", null, false, true, 10),
                fieldDefinition("party_b", "乙方", "PARTY_PATTERN", "(?:乙方|供应商|承包方|受托方)\\s*[：:]\\s*([^\\n，。,；;]{2,40})", null, false, true, 20),
                fieldDefinition("contract_amount", "合同金额", "AMOUNT_PATTERN", "((?:人民币)?\\s*[0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?|(?:人民币)?\\s*[0-9]+(?:\\.[0-9]{1,2})?)\\s*(万元|元)", null, true, true, 30),
                fieldDefinition("effective_date", "生效日期", "DATE_KEYWORD", "(20\\d{2})[年/.-](0?[1-9]|1[0-2])[月/.-](0?[1-9]|[12]\\d|3[01])日?", "生效日期\n签订日期\n签署日期\n合同日期", false, true, 40),
                fieldDefinition("payment_date", "付款日期", "DATE_KEYWORD", "(20\\d{2})[年/.-](0?[1-9]|1[0-2])[月/.-](0?[1-9]|[12]\\d|3[01])日?", "付款日期\n支付日期\n结算日期\n报销日期\n付款时间", true, false, 50),
                fieldDefinition("liability_clause", "责任条款", "KEYWORD_LINE", null, "违约责任\n责任承担\n赔偿责任\n承担责任\n免责", true, false, 60),
                fieldDefinition("reimbursement_item", "报销项目", "KEYWORD_LINE", null, "报销\n费用\n差旅\n交通\n住宿\n发票", true, false, 70)
        );
    }

    private ContractFieldDefinitionEntity fieldDefinition(String fieldCode,
                                                           String fieldName,
                                                           String extractorKind,
                                                           String patternExpr,
                                                           String keywordConfig,
                                                           boolean repeatable,
                                                           boolean deduplicateByNormalized,
                                                           int sortOrder) {
        ContractFieldDefinitionEntity entity = new ContractFieldDefinitionEntity();
        entity.setTenantId(0L);
        entity.setFieldCode(fieldCode);
        entity.setFieldName(fieldName);
        entity.setExtractorKind(extractorKind);
        entity.setPatternExpr(patternExpr);
        entity.setKeywordConfig(keywordConfig);
        entity.setRepeatable(repeatable);
        entity.setDeduplicateByNormalized(deduplicateByNormalized);
        entity.setEnabled(true);
        entity.setSortOrder(sortOrder);
        return entity;
    }

    private ContractRuleDefinitionEntity rule(String code,
                                               ContractRuleType type,
                                               String fieldCode,
                                               ContractRiskSeverity severity,
                                               String params) {
        ContractRuleDefinitionEntity entity = new ContractRuleDefinitionEntity();
        entity.setRuleCode(code);
        entity.setRuleName(code);
        entity.setRuleType(type);
        entity.setFieldCode(fieldCode);
        entity.setSeverity(severity);
        entity.setRuleParams(params);
        entity.setEnabled(true);
        return entity;
    }

    private ExtractedContractField field(String code, String normalizedValue, ContractFieldStatus status) {
        ExtractedContractField field = new ExtractedContractField();
        field.setFieldCode(code);
        field.setFieldName(code);
        field.setRawValue(normalizedValue);
        field.setNormalizedValue(normalizedValue);
        field.setStatus(status);
        field.setConfidence(BigDecimal.valueOf(0.9d));
        field.setEvidenceText(normalizedValue);
        field.setSourceChunkRef("chunk:1");
        field.setExtractorType("test");
        field.setFieldOrder(1);
        return field;
    }
}
