package com.fabbitinc.server.application.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SetProfileImageRequest(
        @Schema(description = "프로필 이미지로 설정할 파일 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        @NotNull(message = "file_id는 필수입니다") UUID fileId
) {
}
