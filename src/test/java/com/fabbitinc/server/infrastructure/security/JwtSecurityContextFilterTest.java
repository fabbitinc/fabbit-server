package com.fabbitinc.server.infrastructure.security;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.AuthPrincipal;
import com.fabbitinc.server.application.auth.support.AuthTokenParser;
import com.fabbitinc.server.application.auth.support.CreateOrgContext;
import com.fabbitinc.server.application.auth.support.CreateOrgPrincipal;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.domain.organization.model.MembershipRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtSecurityContextFilterTest {

    @Mock
    private AuthTokenParser authTokenParser;

    @InjectMocks
    private JwtSecurityContextFilter jwtSecurityContextFilter;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void accessToken이면_authPrincipal을_세팅한다() throws Exception {
        String header = "Bearer access-token";
        AuthContext authContext = new AuthContext(
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                "user@fabbit.com",
                UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
                MembershipRole.ADMIN
        );
        when(authTokenParser.requireAuth(header)).thenReturn(authContext);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/issues");
        request.addHeader(HttpHeaders.AUTHORIZATION, header);

        jwtSecurityContextFilter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertInstanceOf(AuthPrincipal.class, authentication.getPrincipal());
        verify(authTokenParser, never()).requireCreateOrgToken(anyString());
    }

    @Test
    void scopedCreateOrgToken이면_createOrgPrincipal을_세팅한다() throws Exception {
        String header = "Bearer scoped-token";
        CreateOrgContext createOrgContext = new CreateOrgContext(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "create-org@fabbit.com"
        );
        when(authTokenParser.requireAuth(header)).thenThrow(new AppException(ErrorCode.TOKEN_INVALID));
        when(authTokenParser.requireCreateOrgToken(header)).thenReturn(createOrgContext);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/organizations");
        request.addHeader(HttpHeaders.AUTHORIZATION, header);

        jwtSecurityContextFilter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertInstanceOf(CreateOrgPrincipal.class, authentication.getPrincipal());
    }

    @Test
    void 토큰이_모두_유효하지_않으면_인증정보를_세팅하지_않는다() throws Exception {
        String header = "Bearer invalid-token";
        when(authTokenParser.requireAuth(header)).thenThrow(new AppException(ErrorCode.TOKEN_INVALID));
        when(authTokenParser.requireCreateOrgToken(header)).thenThrow(new AppException(ErrorCode.FORBIDDEN));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/issues");
        request.addHeader(HttpHeaders.AUTHORIZATION, header);

        jwtSecurityContextFilter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
