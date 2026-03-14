package com.fabbitinc.server.application.part.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "부품 리비전 승인/릴리즈 요청")
public record PartRevisionChangeReasonRequest(
        @Schema(description = "변경 사유", example = "도면/사양 검토를 마치고 공식 개정본으로 확정합니다")
        @NotBlank(message = "reason은 필수입니다")
        @Size(max = 2000, message = "reason은 최대 2000자여야 합니다")
        String reason
) {
}
