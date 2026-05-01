package com.legal.contract.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.legal.contract.entity.ContractFieldDefinitionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ContractFieldDefinitionMapper extends BaseMapper<ContractFieldDefinitionEntity> {

    @Select("""
            SELECT field_definition_id,
                   tenant_id,
                   field_code,
                   field_name,
                   extractor_kind,
                   pattern_expr,
                   keyword_config,
                   repeatable,
                   deduplicate_by_normalized,
                   enabled,
                   sort_order,
                   description,
                   created_at,
                   updated_at
            FROM contract_field_definition
            WHERE tenant_id = #{tenantId} OR tenant_id = 0
            ORDER BY CASE WHEN tenant_id = #{tenantId} THEN 0 ELSE 1 END,
                     sort_order ASC,
                     field_definition_id ASC
            """)
    List<ContractFieldDefinitionEntity> selectDefinitionsForTenant(@Param("tenantId") Long tenantId);
}
