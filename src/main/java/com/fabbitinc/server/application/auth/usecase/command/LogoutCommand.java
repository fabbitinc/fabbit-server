package com.fabbitinc.server.application.auth.usecase.command;

public record LogoutCommand(
        String refreshToken
) {
}
