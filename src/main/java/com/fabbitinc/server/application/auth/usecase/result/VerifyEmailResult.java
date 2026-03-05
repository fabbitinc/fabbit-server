package com.fabbitinc.server.application.auth.usecase.result;

public record VerifyEmailResult(
        String verificationToken,
        String email
) {
}
