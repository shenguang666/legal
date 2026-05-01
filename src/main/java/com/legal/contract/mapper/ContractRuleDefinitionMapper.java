package com.legal.contract.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.legal.contract.entity.ContractRuleDefinitionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ContractRuleDefinitionMapper extends BaseMapper<ContractRuleDefinitionEntity> {

    @Select("""
            SELECT rule_id,
                   tenant_id,
                   rule_code,
                   rule_name,
                   rule_type,
                   rule_source_type,
                   field_code,
                   document_id,
                   severity,
                   enabled,
                   hit_threshold,
                   rule_content,
                   rule_params,
                   sort_order,
                   created_at,
                   updated_at
            FROM contract_rule_definition
            WHERE enabled = 1
              AND (tenant_id = #{tenantId} OR tenant_id = 0)
            ORDER BY CASE WHEN tenant_id = #{tenantId} THEN 0 ELSE 1 END, sort_order ASC, rule_id ASC
            """)
    List<ContractRuleDefinitionEntity> selectActiveRules(@Param("tenantId") Long tenantId);
}
