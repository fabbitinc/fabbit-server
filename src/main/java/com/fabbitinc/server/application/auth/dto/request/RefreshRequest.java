package com.fabbitinc.server.application.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank(message = "refresh_token은 필수입니다")
        String refreshToken
) {
}
