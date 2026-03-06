package com.fabbitinc.server.presentation.common.web;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공통 API 오류 응답")
public record ApiErrorResponse(
        @Schema(description = "오류 코드", example = "bad_request")
        String code,
        @Schema(description = "오류 메시지", example = "잘못된 요청입니다")
        String message
) {
}
