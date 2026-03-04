package com.fabbitinc.server.application.mapping.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "매핑 검증 이슈")
public record ValidationIssueResponse(
        @Schema(description = "이슈 코드", example = "MISSING_SOURCE_COLUMN")
        String code,
        @Schema(description = "심각도", example = "error")
        String severity,
        @Schema(description = "이슈 메시지")
        String message,
        @Schema(description = "필드 경로")
        String path,
        @Schema(description = "dismiss 용도 사유 코드")
        String dismissedReason
) {
}
