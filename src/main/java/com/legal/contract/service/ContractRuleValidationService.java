package com.legal.contract.service;

import com.legal.config.ElasticsearchProperties;
import com.legal.contract.entity.ContractRuleDefinitionEntity;
import com.legal.contract.mapper.ContractRuleDefinitionMapper;
import com.legal.enums.ContractRuleExecutionStatus;
import com.legal.enums.ContractRuleType;
import com.legal.knowledge.entity.KbChunkEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ContractRuleValidationService {

    private final ContractRuleDefinitionMapper ruleDefinitionMapper;
    private final Map<ContractRuleType, ContractRuleValidator> validators;
    private final RiskRuleDocumentMatchService riskRuleDocumentMatchService;
    private final ElasticsearchProperties elasticsearchProperties;

    public ContractRuleValidationService(ContractRuleDefinitionMapper ruleDefinitionMapper,
                                         List<ContractRuleValidator> validators,
                                         RiskRuleDocumentMatchService riskRuleDocumentMatchService,
                                         ElasticsearchProperties elasticsearchProperties) {
        this.ruleDefinitionMapper = ruleDefinitionMapper;
        this.validators = validators.stream().collect(Collectors.toMap(ContractRuleValidator::supports, Function.identity()));
        this.riskRuleDocumentMatchService = riskRuleDocumentMatchService;
        this.elasticsearchProperties = elasticsearchProperties;
    }

    public List<ContractRuleEvaluation> evaluate(Long tenantId,
                                                  List<KbChunkEntity> reviewChunks,
                                                  List<ExtractedContractField> fields) {
        Map<String, ContractRuleDefinitionEntity> effectiveRules = new LinkedHashMap<>();
        for (ContractRuleDefinitionEntity rule : ruleDefinitionMapper.selectActiveRules(tenantId)) {
            effectiveRules.putIfAbsent(rule.getRuleCode(), rule);
        }
        List<ContractRuleEvaluation> evaluations = new ArrayList<>();
        List<ContractRuleDefinitionEntity> retrievalRules = new ArrayList<>();
        for (ContractRuleDefinitionEntity rule : effectiveRules.values()) {
            if (rule.getRuleType() == ContractRuleType.DOCUMENT_RETRIEVAL) {
                retrievalRules.add(rule);
                continue;
            }
            ContractRuleValidator validator = validators.get(rule.getRuleType());
            if (validator == null) {
                ContractRuleEvaluation evaluation = new ContractRuleEvaluation();
                evaluation.setRuleCode(rule.getRuleCode());
                evaluation.setRuleName(rule.getRuleName());
                evaluation.setRuleType(rule.getRuleType());
                evaluation.setSeverity(rule.getSeverity());
                evaluation.setExecutionStatus(ContractRuleExecutionStatus.SKIPPED);
                evaluation.setMessage("当前规则类型暂无执行器，已跳过");
                evaluation.setAffectedFieldCodes(rule.getFieldCode());
                evaluations.add(evaluation);
                continue;
            }
            evaluations.add(validator.validate(rule, fields));
        }
        evaluations.addAll(riskRuleDocumentMatchService.match(
                tenantId,
                reviewChunks,
                retrievalRules,
                elasticsearchProperties.getIndex().getRiskRule()
        ));
        return evaluations;
    }
}
