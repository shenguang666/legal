package com.legal.contract.service;

import com.legal.contract.entity.ContractRuleDefinitionEntity;
import com.legal.enums.ContractFieldStatus;
import com.legal.enums.ContractRuleExecutionStatus;
import com.legal.enums.ContractRuleType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DateOrderRuleValidator implements ContractRuleValidator {

    private static final Pattern LEFT_FIELD_PATTERN = Pattern.compile("\"leftField\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern RIGHT_FIELD_PATTERN = Pattern.compile("\"rightField\"\\s*:\\s*\"([^\"]+)\"");

    @Override
    public ContractRuleType supports() {
        return ContractRuleType.DATE_ORDER;
    }

    @Override
    public ContractRuleEvaluation validate(ContractRuleDefinitionEntity rule, List<ExtractedContractField> fields) {
        String leftField = readParam(rule.getRuleParams(), LEFT_FIELD_PATTERN, "effective_date");
        String rightField = readParam(rule.getRuleParams(), RIGHT_FIELD_PATTERN, "payment_date");
        LocalDate left = firstDate(fields, leftField);
        LocalDate right = firstDate(fields, rightField);
        ContractRuleEvaluation evaluation = new ContractRuleEvaluation();
        evaluation.setRuleCode(rule.getRuleCode());
        evaluation.setRuleName(rule.getRuleName());
        evaluation.setRuleType(rule.getRuleType());
        evaluation.setSeverity(rule.getSeverity());
        evaluation.setAffectedFieldCodes(leftField + "," + rightField);
        if (left == null || right == null) {
            evaluation.setExecutionStatus(ContractRuleExecutionStatus.SKIPPED);
            evaluation.setMessage("日期字段不完整，跳过顺序校验");
            return evaluation;
        }
        if (right.isBefore(left)) {
            evaluation.setExecutionStatus(ContractRuleExecutionStatus.HIT);
            evaluation.setMessage("付款日期早于生效日期：" + right + " < " + left);
            return evaluation;
        }
        evaluation.setExecutionStatus(ContractRuleExecutionStatus.PASSED);
        evaluation.setMessage("日期顺序校验通过");
        return evaluation;
    }

    private LocalDate firstDate(List<ExtractedContractField> fields, String fieldCode) {
        return fields.stream()
                .filter(field -> fieldCode.equals(field.getFieldCode()))
                .filter(field -> field.getStatus() == ContractFieldStatus.EXTRACTED)
                .filter(field -> StringUtils.hasText(field.getNormalizedValue()))
                .map(field -> LocalDate.parse(field.getNormalizedValue()))
                .findFirst()
                .orElse(null);
    }

    private String readParam(String json, Pattern pattern, String fallback) {
        if (!StringUtils.hasText(json)) {
            return fallback;
        }
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : fallback;
    }
}
