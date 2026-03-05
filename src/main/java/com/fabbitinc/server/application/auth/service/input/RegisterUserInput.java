package com.fabbitinc.server.application.auth.service.input;

public record RegisterUserInput(
        String verificationToken,
        String code,
        String password,
        String fullName
) {
}
