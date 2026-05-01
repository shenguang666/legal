package com.legal.contract.service;

import com.legal.contract.entity.ContractRuleDefinitionEntity;
import com.legal.enums.ContractFieldStatus;
import com.legal.enums.ContractRuleExecutionStatus;
import com.legal.enums.ContractRuleType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AmountConsistencyRuleValidator implements ContractRuleValidator {

    @Override
    public ContractRuleType supports() {
        return ContractRuleType.AMOUNT_CONSISTENCY;
    }

    @Override
    public ContractRuleEvaluation validate(ContractRuleDefinitionEntity rule, List<ExtractedContractField> fields) {
        List<ExtractedContractField> amounts = fields.stream()
                .filter(field -> rule.getFieldCode() != null && rule.getFieldCode().equals(field.getFieldCode()))
                .filter(field -> field.getStatus() == ContractFieldStatus.EXTRACTED)
                .filter(field -> StringUtils.hasText(field.getNormalizedValue()))
                .toList();
        Set<String> values = amounts.stream().map(ExtractedContractField::getNormalizedValue)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        ContractRuleEvaluation evaluation = new ContractRuleEvaluation();
        evaluation.setRuleCode(rule.getRuleCode());
        evaluation.setRuleName(rule.getRuleName());
        evaluation.setRuleType(rule.getRuleType());
        evaluation.setSeverity(rule.getSeverity());
        evaluation.setAffectedFieldCodes(rule.getFieldCode());
        if (values.size() <= 1) {
            evaluation.setExecutionStatus(amounts.isEmpty() ? ContractRuleExecutionStatus.SKIPPED : ContractRuleExecutionStatus.PASSED);
            evaluation.setMessage(amounts.isEmpty() ? "缺少可比较金额，跳过一致性校验" : "金额字段保持一致");
            if (!amounts.isEmpty()) {
                evaluation.setEvidenceText(amounts.get(0).getEvidenceText());
            }
            return evaluation;
        }
        evaluation.setExecutionStatus(ContractRuleExecutionStatus.HIT);
        evaluation.setMessage("识别到不一致金额：" + String.join(" / ", values));
        evaluation.setEvidenceText(amounts.stream().map(ExtractedContractField::getEvidenceText)
                .filter(StringUtils::hasText).limit(2).collect(Collectors.joining(" || ")));
        return evaluation;
    }
}
