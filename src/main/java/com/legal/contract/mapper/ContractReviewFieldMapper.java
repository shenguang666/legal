package com.legal.contract.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.legal.contract.entity.ContractReviewFieldEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ContractReviewFieldMapper extends BaseMapper<ContractReviewFieldEntity> {

    @Select("""
            SELECT field_id,
                   review_id,
                   field_code,
                   field_name,
                   raw_value,
                   normalized_value,
                   status,
                   confidence,
                   evidence_text,
                   source_chunk_ref,
                   extractor_type,
                   field_order,
                   group_key,
                   explanation,
                   created_at
            FROM contract_review_field
            WHERE review_id = #{reviewId}
            ORDER BY field_order ASC, field_id ASC
            """)
    List<ContractReviewFieldEntity> selectByReviewId(@Param("reviewId") Long reviewId);
}
