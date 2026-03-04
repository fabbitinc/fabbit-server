package com.fabbitinc.server.application.issue.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "검토자 요약")
public record ReviewerSummaryResponse(
        UUID userId,
        String fullName,
        String email,
        String phone,
        String profileImageUrl,
        String reviewStatus,
        Instant reviewedAt
) {
}
