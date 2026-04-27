package com.legal.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri == null || !uri.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long latencyMs = System.currentTimeMillis() - start;
            String traceId = MDC.get("traceId");
            String query = request.getQueryString();
            String path = query == null || query.isBlank()
                    ? request.getRequestURI()
                    : request.getRequestURI() + "?" + query;
            log.info("API {} {} -> status={}, latency={}ms, traceId={}",
                    request.getMethod(),
                    path,
                    response.getStatus(),
                    latencyMs,
                    traceId);
        }
    }
}
