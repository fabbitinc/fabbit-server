package com.fabbitinc.server.application.issue.query.result;

import com.fabbitinc.server.domain.issue.model.ReviewStatus;
import java.time.Instant;
import java.util.UUID;

public record ReviewerSummaryResult(
        UUID userId,
        String fullName,
        String email,
        String phone,
        String profileImageUrl,
        ReviewStatus reviewStatus,
        Instant reviewedAt
) {
}
