package com.fabbitinc.server.presentation.organization.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record ProfileImageResponse(
        @Schema(description = "조직 프로필 이미지 URL", example = "https://cdn.example.com/org/profile.png")
        String profileImageUrl
) {
}
