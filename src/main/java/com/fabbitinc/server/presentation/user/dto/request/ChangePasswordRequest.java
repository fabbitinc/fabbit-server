package com.fabbitinc.server.presentation.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @Schema(description = "현재 비밀번호", example = "OldPass123!")
        @NotBlank(message = "current_password는 필수입니다") String currentPassword,

        @Schema(description = "새 비밀번호", example = "NewPass123!")
        @NotBlank(message = "new_password는 필수입니다") @Size(min = 8, max = 128, message = "new_password 길이는 8~128자여야 합니다") String newPassword
) {
}
