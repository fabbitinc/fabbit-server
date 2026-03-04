package com.fabbitinc.server.application.issue.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "리뷰 제출 요청")
public record SubmitReviewRequest(
        @NotBlank
        @Schema(description = "APPROVED 또는 REJECTED")
        String status
) {
}
