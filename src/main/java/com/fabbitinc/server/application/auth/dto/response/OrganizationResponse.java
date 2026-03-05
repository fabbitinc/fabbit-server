package com.fabbitinc.server.application.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record OrganizationResponse(
        @Schema(description = "조직 ID")
        UUID id,
        @Schema(description = "조직 slug", example = "fabbit")
        String slug,
        @Schema(description = "조직 이름", example = "Fabbit")
        String name,
        @Schema(description = "업종", example = "software")
        String industry,
        @Schema(description = "팀 규모", example = "11-50")
        String teamSize,
        @Schema(description = "요금제 타입", example = "STARTER")
        String planType,
        @Schema(description = "조직 프로필 이미지 URL", example = "https://cdn.example.com/org.png")
        String profileImageUrl
) {
}
