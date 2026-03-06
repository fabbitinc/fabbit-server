package com.fabbitinc.server.application.issue.usecase.result;

import com.fabbitinc.server.domain.issue.model.ReviewStatus;

import java.time.Instant;

public record SubmitReviewResult(
        ReviewStatus reviewStatus,
        Instant reviewedAt
) {
}
