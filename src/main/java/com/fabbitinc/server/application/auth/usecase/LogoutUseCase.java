package com.fabbitinc.server.application.auth.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.auth.service.JwtTokenService;
import com.fabbitinc.server.application.auth.usecase.command.LogoutCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class LogoutUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final JwtTokenService jwtTokenService;

    public void execute(LogoutCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        jwtTokenService.revokeAllUserTokens(auth.userId());
    }
}
