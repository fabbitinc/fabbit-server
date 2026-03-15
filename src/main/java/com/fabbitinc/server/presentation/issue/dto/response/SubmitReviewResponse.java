package com.fabbitinc.server.presentation.issue.dto.response;

import com.fabbitinc.server.domain.issue.model.ReviewStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "리뷰 제출 응답")
public record SubmitReviewResponse(
        ReviewStatus reviewStatus,
        Instant reviewedAt
) {
}
