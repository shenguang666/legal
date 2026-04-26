package com.legal.security;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.legal.common.AppException;

public final class AuthContextHolder {

    private AuthContextHolder() {
    }

    public static AuthPrincipal getRequired() {
        StpUtil.checkLogin();
        SaSession session = StpUtil.getSession();
        Long tenantId = parseLong(session.get("tenantId"), "tenantId");
        Long userId = parseLong(session.get("userId"), "userId");
        Object roleValue = session.get("roleCode");
        String roleCode = roleValue == null ? "USER" : String.valueOf(roleValue);
        return new AuthPrincipal(tenantId, userId, roleCode);
    }

    public static void clear() {
        // sa-token 基于 token 管理登录态，无需 ThreadLocal 清理
    }

    private static Long parseLong(Object value, String field) {
        if (value == null) {
            throw AppException.unauthorized("登录态缺少 " + field + " 信息");
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            throw AppException.unauthorized("登录态中的 " + field + " 格式非法");
        }
    }
}
