package com.fabbitinc.server.application.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

public record UpdateProfileResponse(
        @Schema(description = "사용자 ID")
        UUID id,
        @Schema(description = "이메일", example = "user@example.com")
        String email,
        @Schema(description = "이름", example = "홍길동")
        String fullName,
        @Schema(description = "전화번호", example = "010-1234-5678")
        String phone,
        @Schema(description = "프로필 이미지 URL", example = "https://cdn.example.com/user.png")
        String profileImageUrl,
        @Schema(description = "수정 시각")
        Instant updatedAt
) {
}
