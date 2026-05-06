package com.legal.court.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.legal.court.entity.CourtCasePartyEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 智能小法庭案件当事人 Mapper。
 */
@Mapper
public interface CourtCasePartyMapper extends BaseMapper<CourtCasePartyEntity> {
}
