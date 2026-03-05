package com.fabbitinc.server.application.issue.dto.request;

import com.fabbitinc.server.domain.issue.model.ReviewStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "리뷰 제출 요청")
public record SubmitReviewRequest(
        @NotNull(message = "status는 필수입니다")
        @Schema(description = "APPROVED 또는 REJECTED")
        ReviewStatus status
) {
}
