package com.fabbitinc.server.presentation.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record CheckEmailResponse(
        @Schema(description = "사용 가능 여부", example = "false")
        boolean available,
        @Schema(description = "검증 메시지", example = "이미 가입된 이메일입니다")
        String message
) {
}
