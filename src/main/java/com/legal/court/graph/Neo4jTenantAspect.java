package com.legal.court.graph;

import com.legal.security.AuthContextHolder;
import com.legal.security.AuthPrincipal;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 智能小法庭 Neo4j 租户上下文切面。
 */
@Aspect
@Component
public class Neo4jTenantAspect {

    /**
     * court graph 业务服务和仓储入口自动绑定当前登录租户，schema 初始化器等基础设施不走该切面。
     */
    @Around("(within(com.legal.court.graph.service..*) || within(com.legal.court.graph.repository..*)) && !within(com.legal.court.graph.Neo4jSchemaInitializer)")
    public Object bindTenant(ProceedingJoinPoint joinPoint) throws Throwable {
        if (Neo4jTenantContext.hasTenant()) {
            return joinPoint.proceed();
        }
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return Neo4jTenantContext.withTenant(principal.tenantId(), () -> proceed(joinPoint));
    }

    private Object proceed(ProceedingJoinPoint joinPoint) {
        try {
            return joinPoint.proceed();
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new IllegalStateException("Neo4j 租户上下文切面执行失败", ex);
        }
    }
}
