package com.legal.security;

import cn.dev33.satoken.stp.StpInterface;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.legal.auth.entity.LegalUserEntity;
import com.legal.auth.mapper.LegalUserMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SaTokenStpInterface implements StpInterface {

    private final LegalUserMapper legalUserMapper;

    public SaTokenStpInterface(LegalUserMapper legalUserMapper) {
        this.legalUserMapper = legalUserMapper;
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return List.of();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        LegalUserEntity user = legalUserMapper.selectOne(
                new LambdaQueryWrapper<LegalUserEntity>()
                        .eq(LegalUserEntity::getUserId, Long.parseLong(String.valueOf(loginId)))
                        .eq(LegalUserEntity::getStatus, "ACTIVE")
                        .last("limit 1")
        );
        return user == null ? List.of() : List.of(user.getRoleCode());
    }
}
