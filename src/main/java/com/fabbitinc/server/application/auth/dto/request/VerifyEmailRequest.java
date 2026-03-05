package com.fabbitinc.server.application.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyEmailRequest(
        @Schema(description = "인증을 요청한 이메일", example = "user@example.com")
        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "유효한 이메일 형식이 아닙니다")
        String email,

        @Schema(description = "6자리 인증코드", example = "123456")
        @NotBlank(message = "인증코드는 필수입니다")
        @Size(min = 6, max = 6, message = "인증코드는 6자리여야 합니다")
        String code
) {
}
