package com.fabbitinc.server.infrastructure.security;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.AuthPrincipal;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.domain.organization.model.MembershipRole;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityContextCurrentAuthProvider implements CurrentAuthProvider {

    @Override
    public AuthContext getCurrentAuth() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || isAnonymous(authentication)) {
            throw unauthenticated();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthContext authContext) {
            return authContext;
        }
        if (principal instanceof AuthPrincipal authPrincipal) {
            return authPrincipal.toAuthContext();
        }

        throw new AppException(ErrorCode.UNAUTHENTICATED, "인증 컨텍스트를 찾을 수 없습니다");
    }

    @Override
    public AuthContext getCurrentAuth(MembershipRole minimumRole) {
        AuthContext auth = getCurrentAuth();
        if (!auth.role().atLeast(minimumRole)) {
            throw new AppException(ErrorCode.FORBIDDEN, minimumRole.name() + " 이상 권한이 필요합니다");
        }
        return auth;
    }

    @Override
    public AuthContext getAdminAuth() {
        return getCurrentAuth(MembershipRole.ADMIN);
    }

    private boolean isAnonymous(Authentication authentication) {
        return authentication instanceof AnonymousAuthenticationToken
                || "anonymousUser".equals(authentication.getPrincipal());
    }

    private AppException unauthenticated() {
        return new AppException(ErrorCode.UNAUTHENTICATED, "인증이 필요합니다");
    }
}
