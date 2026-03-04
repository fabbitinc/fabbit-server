package com.fabbitinc.server.infrastructure.security;

import com.fabbitinc.server.application.auth.support.AuthPrincipal;
import com.fabbitinc.server.application.tenant.support.TenantContextHolder;
import com.fabbitinc.server.application.tenant.support.TenantSchemaPolicy;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import org.jspecify.annotations.NonNull;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@RequiredArgsConstructor
public class TenantContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String schemaName = resolveSchemaNameFromSecurityContext();

        TenantContextHolder.setCurrentSchema(schemaName);
        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }

    private String resolveSchemaNameFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || isAnonymous(authentication)) {
            return TenantSchemaPolicy.PUBLIC_SCHEMA;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthPrincipal authPrincipal) {
            return TenantSchemaPolicy.schemaNameForOrgId(authPrincipal.orgId());
        }

        return TenantSchemaPolicy.PUBLIC_SCHEMA;
    }

    private boolean isAnonymous(Authentication authentication) {
        return authentication instanceof AnonymousAuthenticationToken
                || "anonymousUser".equals(authentication.getPrincipal());
    }
}
