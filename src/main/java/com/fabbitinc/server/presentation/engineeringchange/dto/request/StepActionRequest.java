package com.fabbitinc.server.presentation.engineeringchange.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "단계 처리 요청")
public record StepActionRequest(
        @NotNull(message = "stepId는 필수입니다") @Schema(description = "처리할 단계 ID")
        UUID stepId,
        @Schema(description = "코멘트 (반려 시 사유)")
        String comment
) {
}
