package com.fabbitinc.server.presentation.part.request;

import com.fabbitinc.server.domain.part.model.PartPreviewSourceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "부품 대표 미리보기 변경 요청")
public record ChangePartPreviewRequest(
        @Schema(description = "대표 미리보기 소스 타입", example = "DRAWING")
        @NotNull(message = "source_type은 필수입니다") PartPreviewSourceType sourceType,
        @Schema(description = "대표 미리보기 소스 ID")
        @NotNull(message = "source_id는 필수입니다") UUID sourceId
) {
}
