package com.fabbitinc.server.application.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record CheckSlugResponse(
        @Schema(description = "사용 가능 여부", example = "true")
        boolean available,
        @Schema(description = "검증 메시지", example = "이미 사용 중인 워크스페이스 주소입니다")
        String message,
        @Schema(description = "대체 slug 제안", example = "fabbit-a1b2")
        String suggestion
) {
    public static CheckSlugResponse asAvailable() {
        return new CheckSlugResponse(true, null, null);
    }
}
