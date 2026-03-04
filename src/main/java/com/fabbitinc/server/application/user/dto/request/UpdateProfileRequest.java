package com.fabbitinc.server.application.user.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(min = 1, max = 100, message = "full_name 길이는 1~100자여야 합니다")
        String fullName,

        @Size(max = 20, message = "phone 길이는 최대 20자입니다")
        String phone
) {
}
