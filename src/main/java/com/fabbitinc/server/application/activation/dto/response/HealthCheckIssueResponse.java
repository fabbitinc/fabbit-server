package com.fabbitinc.server.application.activation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "헬스체크 이슈 항목")
public record HealthCheckIssueResponse(
        @Schema(description = "이슈 분류", example = "orphan_parts")
        HealthIssueCategory category,
        @Schema(description = "이슈 심각도", example = "warning")
        HealthIssueSeverity severity,
        @Schema(description = "이슈 메시지", example = "부모 없이 고립된 Part가 존재합니다")
        String message,
        @Schema(description = "영향 건수", example = "3")
        int count
) {
}
