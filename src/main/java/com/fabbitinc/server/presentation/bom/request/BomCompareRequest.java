package com.fabbitinc.server.presentation.bom.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "BOM 비교 요청")
public record BomCompareRequest(

        @Schema(description = "소스 부품 리비전 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        @NotNull(message = "소스 부품 리비전 ID는 필수입니다")
        UUID sourceRevisionId,

        @Schema(description = "대상 부품 리비전 ID", example = "550e8400-e29b-41d4-a716-446655440001")
        @NotNull(message = "대상 부품 리비전 ID는 필수입니다")
        UUID targetRevisionId
) {
}
