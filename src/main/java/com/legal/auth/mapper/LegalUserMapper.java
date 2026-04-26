package com.legal.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.legal.auth.entity.LegalUserEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LegalUserMapper extends BaseMapper<LegalUserEntity> {
}
