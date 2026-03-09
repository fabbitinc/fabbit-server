package com.fabbitinc.server.presentation.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SendVerificationRequest(
        @Schema(description = "인증코드를 발송할 이메일", example = "user@example.com")
        @NotBlank(message = "이메일은 필수입니다") @Email(message = "유효한 이메일 형식이 아닙니다") String email,
        @Schema(description = "봇 방지 토큰", example = "token-example")
        String turnstileToken
) {
}
