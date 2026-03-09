package com.fabbitinc.server.presentation.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record SiteResponse(
        @Schema(description = "워크스페이스 slug", example = "fabbit")
        String slug,
        @Schema(description = "워크스페이스 이름", example = "Fabbit")
        String name,
        @Schema(description = "워크스페이스 프로필 이미지 URL", example = "https://cdn.example.com/org.png")
        String profileImageUrl
) {
}
