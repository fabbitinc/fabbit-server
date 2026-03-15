package com.fabbitinc.server.presentation.issue.dto.response;

import com.fabbitinc.server.domain.issue.model.ReviewStatus;
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
        ReviewStatus reviewStatus,
        Instant reviewedAt
) {
}
