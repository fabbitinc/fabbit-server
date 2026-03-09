package com.fabbitinc.server.presentation.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record SendVerificationResponse(
        @Schema(description = "응답 메시지", example = "인증코드가 발송되었습니다")
        String message
) {
}
