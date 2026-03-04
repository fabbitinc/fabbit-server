package com.fabbitinc.server.application.auth.dto.response;

public record VerifyEmailResponse(
        String verificationToken,
        String email
) {
}
