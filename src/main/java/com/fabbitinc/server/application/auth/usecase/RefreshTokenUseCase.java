package com.fabbitinc.server.application.auth.usecase;

import com.fabbitinc.server.application.auth.service.JwtTokenService;
import com.fabbitinc.server.application.auth.usecase.command.RefreshTokenCommand;
import com.fabbitinc.server.application.auth.usecase.result.AuthTokenResult;
import com.fabbitinc.server.application.auth.usecase.result.RefreshTokenResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class RefreshTokenUseCase {

    private final JwtTokenService jwtTokenService;

    public RefreshTokenResult execute(RefreshTokenCommand command) {
        JwtTokenService.IssuedTokens tokens = jwtTokenService.refreshTokenBundle(command.refreshToken());
        return new RefreshTokenResult(
                new AuthTokenResult(tokens.accessToken(), tokens.refreshToken(), tokens.tokenType())
        );
    }
}
