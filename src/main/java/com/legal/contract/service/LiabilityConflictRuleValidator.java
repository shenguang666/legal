package com.legal.contract.service;

import com.legal.contract.entity.ContractRuleDefinitionEntity;
import com.legal.enums.ContractFieldStatus;
import com.legal.enums.ContractRuleExecutionStatus;
import com.legal.enums.ContractRuleType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
public class LiabilityConflictRuleValidator implements ContractRuleValidator {

    @Override
    public ContractRuleType supports() {
        return ContractRuleType.LIABILITY_CONFLICT;
    }

    @Override
    public ContractRuleEvaluation validate(ContractRuleDefinitionEntity rule, List<ExtractedContractField> fields) {
        List<ExtractedContractField> clauses = fields.stream()
                .filter(field -> rule.getFieldCode() != null && rule.getFieldCode().equals(field.getFieldCode()))
                .filter(field -> field.getStatus() == ContractFieldStatus.EXTRACTED)
                .toList();
        ContractRuleEvaluation evaluation = new ContractRuleEvaluation();
        evaluation.setRuleCode(rule.getRuleCode());
        evaluation.setRuleName(rule.getRuleName());
        evaluation.setRuleType(rule.getRuleType());
        evaluation.setSeverity(rule.getSeverity());
        evaluation.setAffectedFieldCodes(rule.getFieldCode());
        if (clauses.isEmpty()) {
            evaluation.setExecutionStatus(ContractRuleExecutionStatus.SKIPPED);
            evaluation.setMessage("缺少责任条款，跳过冲突校验");
            return evaluation;
        }
        boolean hasDisclaimer = clauses.stream().map(ExtractedContractField::getNormalizedValue)
                .filter(StringUtils::hasText)
                .anyMatch(value -> value.contains("免责") || value.contains("不承担") || value.contains("免于承担"));
        boolean hasLiability = clauses.stream().map(ExtractedContractField::getNormalizedValue)
                .filter(StringUtils::hasText)
                .anyMatch(value -> value.contains("违约责任") || value.contains("赔偿") || value.contains("承担责任") || value.contains("承担全部责任"));
        if (hasDisclaimer && hasLiability) {
            evaluation.setExecutionStatus(ContractRuleExecutionStatus.HIT);
            evaluation.setMessage("责任条款前后存在矛盾，请人工复核");
            evaluation.setEvidenceText(clauses.stream().map(ExtractedContractField::getEvidenceText)
                    .filter(StringUtils::hasText).limit(2).reduce((a, b) -> a + " || " + b).orElse(null));
            return evaluation;
        }
        evaluation.setExecutionStatus(ContractRuleExecutionStatus.PASSED);
        evaluation.setMessage("未发现明显责任条款冲突");
        return evaluation;
    }
}
