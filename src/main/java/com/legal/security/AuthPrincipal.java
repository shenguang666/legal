package com.legal.security;

public record AuthPrincipal(Long tenantId, Long userId, String role) {
}
