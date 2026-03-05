package com.fabbitinc.server.application.auth.usecase.result;

public record AuthTokenResult(
        String accessToken,
        String refreshToken,
        String tokenType
) {
}
