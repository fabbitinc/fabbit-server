package com.fabbitinc.server.application.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "verification_token은 필수입니다")
        String verificationToken,

        @NotBlank(message = "code는 필수입니다")
        @Size(min = 6, max = 6, message = "code는 6자리여야 합니다")
        String code,

        @NotBlank(message = "password는 필수입니다")
        @Size(min = 8, max = 128, message = "password 길이는 8~128자여야 합니다")
        String password,

        @NotBlank(message = "full_name은 필수입니다")
        @Size(min = 1, max = 100, message = "full_name 길이는 1~100자여야 합니다")
        String fullName,

        @NotBlank(message = "org_name은 필수입니다")
        @Size(min = 1, max = 100, message = "org_name 길이는 1~100자여야 합니다")
        String orgName,

        @Size(min = 3, max = 50, message = "slug 길이는 3~50자여야 합니다")
        String slug,
        String industry,
        String teamSize,
        String planType,
        String turnstileToken
) {
}
