package com.fabbitinc.server.infrastructure.security;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.AuthPrincipal;
import com.fabbitinc.server.application.auth.support.AuthTokenParser;
import com.fabbitinc.server.application.auth.support.CreateOrgContext;
import com.fabbitinc.server.application.auth.support.CreateOrgPrincipal;
import com.fabbitinc.server.application.common.exception.AppException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtSecurityContextFilter extends OncePerRequestFilter {

    public static final String AUTH_FAILURE_ATTRIBUTE = JwtSecurityContextFilter.class.getName() + ".AUTH_FAILURE";

    private final AuthTokenParser authTokenParser;

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        populateSecurityContext(request, authorizationHeader);
        filterChain.doFilter(request, response);
    }

    private void populateSecurityContext(HttpServletRequest request, String authorizationHeader) {
        AppException authFailure;
        try {
            AuthContext authContext = authTokenParser.requireAuth(authorizationHeader);
            setAuthPrincipal(authContext);
            return;
        } catch (AppException ex) {
            authFailure = ex;
        }

        try {
            CreateOrgContext createOrgContext = authTokenParser.requireCreateOrgToken(authorizationHeader);
            setCreateOrgPrincipal(createOrgContext);
        } catch (AppException ignored) {
            SecurityContextHolder.clearContext();
            request.setAttribute(AUTH_FAILURE_ATTRIBUTE, authFailure);
        }
    }

    private void setAuthPrincipal(AuthContext authContext) {
        AuthPrincipal principal = new AuthPrincipal(
                authContext.userId(),
                authContext.email(),
                authContext.orgId(),
                authContext.role()
        );
        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                AuthorityUtils.createAuthorityList("ROLE_" + authContext.role().name())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void setCreateOrgPrincipal(CreateOrgContext createOrgContext) {
        CreateOrgPrincipal principal = new CreateOrgPrincipal(
                createOrgContext.userId(),
                createOrgContext.email()
        );
        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                AuthorityUtils.createAuthorityList("SCOPE_create_org")
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
