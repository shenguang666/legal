package com.legal.contract.service;

import com.legal.contract.entity.ContractRuleDefinitionEntity;
import com.legal.enums.ContractFieldStatus;
import com.legal.enums.ContractRuleExecutionStatus;
import com.legal.enums.ContractRuleType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AmountThresholdRuleValidator implements ContractRuleValidator {

    private static final Pattern MAX_AMOUNT_PATTERN = Pattern.compile("\"maxAmount\"\\s*:\\s*\"([^\"]+)\"");

    @Override
    public ContractRuleType supports() {
        return ContractRuleType.AMOUNT_THRESHOLD;
    }

    @Override
    public ContractRuleEvaluation validate(ContractRuleDefinitionEntity rule, List<ExtractedContractField> fields) {
        BigDecimal maxAmount = readMaxAmount(rule.getRuleParams());
        ContractRuleEvaluation evaluation = new ContractRuleEvaluation();
        evaluation.setRuleCode(rule.getRuleCode());
        evaluation.setRuleName(rule.getRuleName());
        evaluation.setRuleType(rule.getRuleType());
        evaluation.setSeverity(rule.getSeverity());
        evaluation.setAffectedFieldCodes(rule.getFieldCode());
        if (maxAmount == null) {
            evaluation.setExecutionStatus(ContractRuleExecutionStatus.SKIPPED);
            evaluation.setMessage("规则未配置阈值，跳过校验");
            return evaluation;
        }
        List<ExtractedContractField> amounts = fields.stream()
                .filter(field -> rule.getFieldCode() != null && rule.getFieldCode().equals(field.getFieldCode()))
                .filter(field -> field.getStatus() == ContractFieldStatus.EXTRACTED)
                .filter(field -> StringUtils.hasText(field.getNormalizedValue()))
                .toList();
        if (amounts.isEmpty()) {
            evaluation.setExecutionStatus(ContractRuleExecutionStatus.SKIPPED);
            evaluation.setMessage("缺少可比较金额，跳过阈值校验");
            return evaluation;
        }
        for (ExtractedContractField amount : amounts) {
            BigDecimal value = new BigDecimal(amount.getNormalizedValue());
            if (value.compareTo(maxAmount) > 0) {
                evaluation.setExecutionStatus(ContractRuleExecutionStatus.HIT);
                evaluation.setMessage("金额超过阈值：" + value.toPlainString() + " > " + maxAmount.toPlainString());
                evaluation.setEvidenceText(amount.getEvidenceText());
                return evaluation;
            }
        }
        evaluation.setExecutionStatus(ContractRuleExecutionStatus.PASSED);
        evaluation.setMessage("金额阈值校验通过");
        return evaluation;
    }

    private BigDecimal readMaxAmount(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        Matcher matcher = MAX_AMOUNT_PATTERN.matcher(json);
        if (!matcher.find()) {
            return null;
        }
        return new BigDecimal(matcher.group(1));
    }
}
