package com.fabbitinc.server.application.auth.usecase.command;

public record RefreshTokenCommand(
        String refreshToken
) {
}
