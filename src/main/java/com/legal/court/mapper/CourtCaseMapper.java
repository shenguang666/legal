package com.legal.court.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.legal.court.entity.CourtCaseEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 智能小法庭案件 Mapper。
 */
@Mapper
public interface CourtCaseMapper extends BaseMapper<CourtCaseEntity> {

    /**
     * 累加案件 Token 总消耗。
     */
    @Update("""
            UPDATE court_case
            SET total_tokens = COALESCE(total_tokens, 0) + #{tokenUsage},
                updated_at = NOW()
            WHERE case_id = #{caseId}
              AND tenant_id = #{tenantId}
            """)
    int increaseTotalTokens(@Param("tenantId") Long tenantId,
                            @Param("caseId") Long caseId,
                            @Param("tokenUsage") Integer tokenUsage);
}
