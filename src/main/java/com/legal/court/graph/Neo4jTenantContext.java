package com.legal.court.graph;

import com.legal.common.AppException;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 智能小法庭 Neo4j 租户上下文。
 */
public final class Neo4jTenantContext {

    /** 当前线程绑定的租户ID。 */
    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();

    private Neo4jTenantContext() {
    }

    /**
     * 绑定租户并执行回调，执行结束后自动恢复原上下文。
     */
    public static <T> T withTenant(Long tenantId, Supplier<T> supplier) {
        Long previous = TENANT_ID.get();
        bind(tenantId);
        try {
            return supplier.get();
        } finally {
            restore(previous);
        }
    }

    /**
     * 绑定租户并执行回调，执行结束后自动恢复原上下文。
     */
    public static void withTenant(Long tenantId, Runnable runnable) {
        withTenant(tenantId, () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * 获取当前线程的必填租户ID。
     */
    public static Long requiredTenantId() {
        Long tenantId = TENANT_ID.get();
        if (tenantId == null) {
            throw AppException.badRequest("Neo4j 查询缺少租户上下文");
        }
        return tenantId;
    }

    /**
     * 判断当前线程是否已经绑定租户ID。
     */
    public static boolean hasTenant() {
        return TENANT_ID.get() != null;
    }

    /**
     * 为命名 Cypher 查询追加 tenantId 参数。
     */
    public static Map<String, Object> withTenantParam(Map<String, Object> params) {
        Map<String, Object> merged = new HashMap<>(params == null ? Map.of() : params);
        Long tenantId = requiredTenantId();
        Object existing = merged.putIfAbsent("tenantId", tenantId);
        if (existing != null && !tenantId.equals(asLong(existing))) {
            throw AppException.badRequest("Neo4j 查询租户参数与上下文不一致");
        }
        return merged;
    }

    /**
     * 校验命名 Cypher 是否显式使用 tenantId 参数。
     */
    public static void requireTenantParameter(String cypher) {
        if (cypher == null || !cypher.contains("$tenantId")) {
            throw AppException.badRequest("Neo4j 查询必须使用命名 Cypher 并显式包含 $tenantId 参数");
        }
    }

    private static void bind(Long tenantId) {
        if (tenantId == null) {
            throw AppException.badRequest("Neo4j 租户上下文不能为空");
        }
        TENANT_ID.set(tenantId);
    }

    private static void restore(Long previous) {
        if (previous == null) {
            TENANT_ID.remove();
        } else {
            TENANT_ID.set(previous);
        }
    }

    private static Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            throw AppException.badRequest("Neo4j 查询租户参数格式非法");
        }
    }
}
