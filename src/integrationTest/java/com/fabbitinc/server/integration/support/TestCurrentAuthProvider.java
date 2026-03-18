package com.fabbitinc.server.integration.support;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;

public class TestCurrentAuthProvider implements CurrentAuthProvider {

    private final ThreadLocal<AuthContext> currentAuth = new ThreadLocal<>();

    @Override
    public AuthContext getCurrentAuth() {
        AuthContext authContext = currentAuth.get();
        if (authContext == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED, "테스트 인증 컨텍스트가 없습니다");
        }
        return authContext;
    }

    public void set(AuthContext authContext) {
        currentAuth.set(authContext);
    }

    public void clear() {
        currentAuth.remove();
    }
}
