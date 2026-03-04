package com.fabbitinc.server.application.auth.usecase;

import com.fabbitinc.server.application.auth.dto.request.RefreshRequest;
import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.AuthTokenParser;
import com.fabbitinc.server.application.auth.service.JwtTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class LogoutUseCase {

    private final AuthTokenParser authTokenParser;
    private final JwtTokenService jwtTokenService;

    @Transactional
    public void execute(String authorizationHeader, RefreshRequest request) {
        AuthContext auth = authTokenParser.requireAuth(authorizationHeader);
        jwtTokenService.revokeAllUserTokens(auth.userId());
    }
}
