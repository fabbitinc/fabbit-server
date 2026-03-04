package com.fabbitinc.server.infrastructure.security;

import com.fabbitinc.server.application.auth.support.CreateOrgContext;
import com.fabbitinc.server.application.auth.support.CreateOrgPrincipal;
import com.fabbitinc.server.application.auth.support.CurrentCreateOrgProvider;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityContextCurrentCreateOrgProvider implements CurrentCreateOrgProvider {

    @Override
    public CreateOrgContext getCurrentCreateOrg() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || isAnonymous(authentication)) {
            throw unauthenticated();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof CreateOrgPrincipal createOrgPrincipal) {
            return createOrgPrincipal.toCreateOrgContext();
        }

        throw new AppException(ErrorCode.FORBIDDEN, "조직 생성 권한이 필요합니다");
    }

    private boolean isAnonymous(Authentication authentication) {
        return authentication instanceof AnonymousAuthenticationToken
                || "anonymousUser".equals(authentication.getPrincipal());
    }

    private AppException unauthenticated() {
        return new AppException(ErrorCode.UNAUTHENTICATED, "인증이 필요합니다");
    }
}
