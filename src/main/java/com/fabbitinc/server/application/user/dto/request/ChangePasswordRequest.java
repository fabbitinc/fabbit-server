package com.fabbitinc.server.application.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "current_password는 필수입니다")
        String currentPassword,

        @NotBlank(message = "new_password는 필수입니다")
        @Size(min = 8, max = 128, message = "new_password 길이는 8~128자여야 합니다")
        String newPassword
) {
}
