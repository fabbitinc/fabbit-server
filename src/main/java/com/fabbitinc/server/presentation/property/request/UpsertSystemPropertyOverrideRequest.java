package com.fabbitinc.server.presentation.property.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

@Schema(description = "시스템 속성 override 수정 요청")
public record UpsertSystemPropertyOverrideRequest(
        @Schema(description = "표시명 override", example = "품목군")
        @Size(max = 200, message = "display_name_override는 최대 200자여야 합니다")
        String displayNameOverride,

        @Schema(description = "표시 순서 override", example = "70")
        @Min(value = 0, message = "display_order는 0 이상이어야 합니다")
        Integer displayOrder,

        @Schema(description = "활성 여부", example = "true")
        Boolean active
) {
}
