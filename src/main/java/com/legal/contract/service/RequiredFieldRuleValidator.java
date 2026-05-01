package com.legal.contract.service;

import com.legal.contract.entity.ContractRuleDefinitionEntity;
import com.legal.enums.ContractFieldStatus;
import com.legal.enums.ContractRuleExecutionStatus;
import com.legal.enums.ContractRuleType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RequiredFieldRuleValidator implements ContractRuleValidator {

    @Override
    public ContractRuleType supports() {
        return ContractRuleType.REQUIRED_FIELD;
    }

    @Override
    public ContractRuleEvaluation validate(ContractRuleDefinitionEntity rule, List<ExtractedContractField> fields) {
        boolean matched = fields.stream()
                .anyMatch(field -> rule.getFieldCode() != null
                        && rule.getFieldCode().equals(field.getFieldCode())
                        && field.getStatus() == ContractFieldStatus.EXTRACTED);
        ContractRuleEvaluation evaluation = new ContractRuleEvaluation();
        evaluation.setRuleCode(rule.getRuleCode());
        evaluation.setRuleName(rule.getRuleName());
        evaluation.setRuleType(rule.getRuleType());
        evaluation.setSeverity(rule.getSeverity());
        evaluation.setAffectedFieldCodes(rule.getFieldCode());
        if (matched) {
            evaluation.setExecutionStatus(ContractRuleExecutionStatus.PASSED);
            evaluation.setMessage("字段已识别，通过必填校验");
        } else {
            evaluation.setExecutionStatus(ContractRuleExecutionStatus.HIT);
            evaluation.setMessage("缺少必填字段：" + rule.getFieldCode());
        }
        return evaluation;
    }
}
