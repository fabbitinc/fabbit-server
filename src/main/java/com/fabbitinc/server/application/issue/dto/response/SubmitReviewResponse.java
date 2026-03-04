package com.fabbitinc.server.application.issue.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "리뷰 제출 응답")
public record SubmitReviewResponse(
        String reviewStatus,
        Instant reviewedAt
) {
}
