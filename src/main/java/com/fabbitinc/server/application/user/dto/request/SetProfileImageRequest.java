package com.fabbitinc.server.application.user.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SetProfileImageRequest(
        @NotNull(message = "file_id는 필수입니다")
        UUID fileId
) {
}
