package com.legal.contract.service;

import com.legal.contract.entity.ContractRuleDefinitionEntity;
import com.legal.enums.ContractRuleType;

import java.util.List;

public interface ContractRuleValidator {

    ContractRuleType supports();

    ContractRuleEvaluation validate(ContractRuleDefinitionEntity rule,
                                    List<ExtractedContractField> fields);
}
