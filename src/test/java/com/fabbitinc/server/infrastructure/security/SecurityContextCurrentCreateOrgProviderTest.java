package com.fabbitinc.server.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fabbitinc.server.application.auth.support.CreateOrgContext;
import com.fabbitinc.server.application.auth.support.CreateOrgPrincipal;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityContextCurrentCreateOrgProviderTest {

    private final SecurityContextCurrentCreateOrgProvider currentCreateOrgProvider =
            new SecurityContextCurrentCreateOrgProvider();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createOrgPrincipal을_context로_변환해_반환한다() {
        CreateOrgPrincipal createOrgPrincipal = new CreateOrgPrincipal(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "create-org@fabbit.com"
        );
        TestingAuthenticationToken authentication =
                new TestingAuthenticationToken(createOrgPrincipal, null, "ROLE_SCOPED");
        setAuthentication(authentication);

        CreateOrgContext result = currentCreateOrgProvider.getCurrentCreateOrg();

        assertEquals(createOrgPrincipal.toCreateOrgContext(), result);
    }

    @Test
    void 인증정보가_없으면_UNAUTHENTICATED를_던진다() {
        AppException ex = assertThrows(AppException.class, currentCreateOrgProvider::getCurrentCreateOrg);

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

        AppException ex = assertThrows(AppException.class, currentCreateOrgProvider::getCurrentCreateOrg);

        assertEquals(ErrorCode.UNAUTHENTICATED, ex.getErrorCode());
    }

    @Test
    void 지원하지_않는_principal_타입이면_FORBIDDEN을_던진다() {
        TestingAuthenticationToken authentication =
                new TestingAuthenticationToken("unsupported-principal", null, "ROLE_USER");
        setAuthentication(authentication);

        AppException ex = assertThrows(AppException.class, currentCreateOrgProvider::getCurrentCreateOrg);

        assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
    }

    private void setAuthentication(org.springframework.security.core.Authentication authentication) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }
}
