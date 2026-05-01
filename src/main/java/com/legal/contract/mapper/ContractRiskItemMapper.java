package com.legal.contract.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.legal.contract.entity.ContractRiskItemEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ContractRiskItemMapper extends BaseMapper<ContractRiskItemEntity> {

    @Select("""
            SELECT risk_id,
                   review_id,
                   rule_code,
                   rule_name,
                   rule_type,
                   severity,
                   execution_status,
                   message,
                   evidence_text,
                   affected_field_codes,
                   created_at
            FROM contract_risk_item
            WHERE review_id = #{reviewId}
            ORDER BY risk_id ASC
            """)
    List<ContractRiskItemEntity> selectByReviewId(@Param("reviewId") Long reviewId);
}
