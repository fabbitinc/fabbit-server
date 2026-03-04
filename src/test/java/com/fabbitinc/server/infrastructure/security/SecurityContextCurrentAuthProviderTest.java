package com.fabbitinc.server.infrastructure.security;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.AuthPrincipal;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.domain.organization.model.MembershipRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SecurityContextCurrentAuthProviderTest {

    private final SecurityContextCurrentAuthProvider currentAuthProvider = new SecurityContextCurrentAuthProvider();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authPrincipal을_authContext로_변환해_반환한다() {
        AuthPrincipal authPrincipal = new AuthPrincipal(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "principal@fabbit.com",
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                MembershipRole.OWNER
        );
        TestingAuthenticationToken authentication =
                new TestingAuthenticationToken(authPrincipal, null, "ROLE_OWNER");
        setAuthentication(authentication);

        AuthContext result = currentAuthProvider.getCurrentAuth();

        assertEquals(authPrincipal.toAuthContext(), result);
    }

    @Test
    void 인증정보가_없으면_UNAUTHENTICATED를_던진다() {
        AppException ex = assertThrows(AppException.class, currentAuthProvider::getCurrentAuth);

        assertEquals(ErrorCode.UNAUTHENTICATED, ex.getErrorCode());
    }

    @Test
    void 익명_인증이면_UNAUTHENTICATED를_던진다() {
        AnonymousAuthenticationToken anonymousAuthenticationToken = new AnonymousAuthenticationToken(
                "anonymous-key",
                "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")
        );
        setAuthentication(anonymousAuthenticationToken);

        AppException ex = assertThrows(AppException.class, currentAuthProvider::getCurrentAuth);

        assertEquals(ErrorCode.UNAUTHENTICATED, ex.getErrorCode());
    }

    @Test
    void 지원하지_않는_principal_타입이면_UNAUTHENTICATED를_던진다() {
        TestingAuthenticationToken authentication =
                new TestingAuthenticationToken("unsupported-principal", null, "ROLE_USER");
        setAuthentication(authentication);

        AppException ex = assertThrows(AppException.class, currentAuthProvider::getCurrentAuth);

        assertEquals(ErrorCode.UNAUTHENTICATED, ex.getErrorCode());
    }

    private void setAuthentication(org.springframework.security.core.Authentication authentication) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }
}
