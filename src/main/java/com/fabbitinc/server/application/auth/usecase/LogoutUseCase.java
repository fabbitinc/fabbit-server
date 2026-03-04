package com.fabbitinc.server.application.auth.usecase;

import com.fabbitinc.server.application.auth.dto.request.RefreshRequest;
import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.auth.service.JwtTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class LogoutUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final JwtTokenService jwtTokenService;

    @Transactional
    public void execute(RefreshRequest request) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        jwtTokenService.revokeAllUserTokens(auth.userId());
    }
}
