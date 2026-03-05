package com.fabbitinc.server.application.auth.usecase.command;

public record VerifyEmailCommand(
        String email,
        String code
) {
}
