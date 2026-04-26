package com.legal.auth.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.legal.auth.dto.LoginRequest;
import com.legal.auth.dto.LoginResponse;
import com.legal.auth.entity.LegalUserEntity;
import com.legal.auth.mapper.LegalUserMapper;
import com.legal.common.AppException;
import com.legal.security.PasswordCodec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final LegalUserMapper legalUserMapper;

    public AuthService(LegalUserMapper legalUserMapper) {
        this.legalUserMapper = legalUserMapper;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        LegalUserEntity user = legalUserMapper.selectOne(
                new LambdaQueryWrapper<LegalUserEntity>()
                        .eq(LegalUserEntity::getTenantId, request.getTenantId())
                        .eq(LegalUserEntity::getUsername, request.getUsername())
                        .eq(LegalUserEntity::getStatus, "ACTIVE")
                        .last("limit 1")
        );
        if (user == null || !user.getPasswordHash().equalsIgnoreCase(PasswordCodec.sha256Hex(request.getPassword()))) {
            throw AppException.unauthorized("用户名或密码错误");
        }

        StpUtil.login(user.getUserId());
        StpUtil.getSession().set("tenantId", user.getTenantId());
        StpUtil.getSession().set("userId", user.getUserId());
        StpUtil.getSession().set("username", user.getUsername());
        StpUtil.getSession().set("displayName", user.getDisplayName());
        StpUtil.getSession().set("roleCode", user.getRoleCode());

        user.setLastLoginAt(LocalDateTime.now());
        legalUserMapper.updateById(user);
        return toResponse(user, StpUtil.getTokenValue());
    }

    public LoginResponse currentUser() {
        StpUtil.checkLogin();
        LegalUserEntity user = legalUserMapper.selectOne(
                new LambdaQueryWrapper<LegalUserEntity>()
                        .eq(LegalUserEntity::getUserId, StpUtil.getLoginIdAsLong())
                        .eq(LegalUserEntity::getStatus, "ACTIVE")
                        .last("limit 1")
        );
        if (user == null) {
            throw AppException.unauthorized("当前登录用户不存在或已停用");
        }
        return toResponse(user, StpUtil.getTokenValue());
    }

    public void logout() {
        if (StpUtil.isLogin()) {
            StpUtil.logout();
        }
    }

    private LoginResponse toResponse(LegalUserEntity user, String token) {
        LoginResponse response = new LoginResponse();
        response.setUserId(user.getUserId());
        response.setTenantId(user.getTenantId());
        response.setUsername(user.getUsername());
        response.setDisplayName(user.getDisplayName());
        response.setRoleCode(user.getRoleCode());
        response.setToken(token);
        return response;
    }
}
