package com.fabbitinc.server.application.auth.usecase.command;

public record SendVerificationCommand(
        String email,
        String turnstileToken
) {
}
