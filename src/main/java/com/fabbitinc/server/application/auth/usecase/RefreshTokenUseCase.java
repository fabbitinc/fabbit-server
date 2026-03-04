package com.fabbitinc.server.application.auth.usecase;

import com.fabbitinc.server.application.auth.dto.request.RefreshRequest;
import com.fabbitinc.server.application.auth.dto.response.TokenResponse;
import com.fabbitinc.server.application.auth.service.JwtTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class RefreshTokenUseCase {

    private final JwtTokenService jwtTokenService;

    @Transactional
    public TokenResponse execute(RefreshRequest request) {
        return jwtTokenService.refreshTokens(request.refreshToken());
    }
}
