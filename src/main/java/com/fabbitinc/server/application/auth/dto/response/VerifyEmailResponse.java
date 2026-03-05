package com.fabbitinc.server.application.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record VerifyEmailResponse(
        @Schema(description = "인증 완료 후 발급된 검증 토큰", example = "e9b6c2a9c0d24f9b8d99f5f5d67f73be")
        String verificationToken,
        @Schema(description = "인증 완료된 이메일", example = "user@example.com")
        String email
) {
}
