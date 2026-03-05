package com.fabbitinc.server.application.auth.usecase.result;

public record RefreshTokenResult(
        AuthTokenResult tokens
) {
}
