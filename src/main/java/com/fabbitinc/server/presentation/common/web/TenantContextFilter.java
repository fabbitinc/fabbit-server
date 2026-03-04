package com.fabbitinc.server.presentation.common.web;

import com.fabbitinc.server.application.auth.support.AuthTokenParser;
import com.fabbitinc.server.application.tenant.support.TenantContextHolder;
import com.fabbitinc.server.application.tenant.support.TenantSchemaPolicy;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import org.jspecify.annotations.NonNull;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class TenantContextFilter extends OncePerRequestFilter {

    private final AuthTokenParser authTokenParser;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorizationHeader = request.getHeader("Authorization");
        String schemaName = authTokenParser.resolveOrgIdForTenant(authorizationHeader)
                .map(this::toTenantSchemaName)
                .orElse(TenantSchemaPolicy.PUBLIC_SCHEMA);

        TenantContextHolder.setCurrentSchema(schemaName);
        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }

    private String toTenantSchemaName(UUID orgId) {
        return TenantSchemaPolicy.schemaNameForOrgId(orgId);
    }
}
