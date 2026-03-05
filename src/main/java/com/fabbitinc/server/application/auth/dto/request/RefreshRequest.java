package com.fabbitinc.server.application.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @Schema(description = "리프레시 토큰", example = "eyJhbGciOiJIUzI1NiJ9...")
        @NotBlank(message = "refresh_token은 필수입니다")
        String refreshToken
) {
}
