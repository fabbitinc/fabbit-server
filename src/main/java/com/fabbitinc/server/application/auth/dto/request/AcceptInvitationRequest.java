package com.fabbitinc.server.application.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AcceptInvitationRequest(
        @NotBlank(message = "token은 필수입니다")
        String token,

        @Size(min = 8, max = 128, message = "password 길이는 8~128자여야 합니다")
        String password,

        @Size(min = 1, max = 100, message = "full_name 길이는 1~100자여야 합니다")
        String fullName
) {
}
