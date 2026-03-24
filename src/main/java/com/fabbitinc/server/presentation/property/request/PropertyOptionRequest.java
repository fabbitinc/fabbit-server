package com.fabbitinc.server.presentation.property.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "속성 옵션 요청")
public record PropertyOptionRequest(
        @Schema(description = "옵션 값", example = "AL6061")
        @NotBlank(message = "value는 필수입니다") @Size(max = 100, message = "value는 최대 100자여야 합니다") String value,

        @Schema(description = "옵션 표시명", example = "AL6061")
        @NotBlank(message = "label은 필수입니다") @Size(max = 200, message = "label은 최대 200자여야 합니다") String label,

        @Schema(description = "표시 순서", example = "10")
        Integer displayOrder,

        @Schema(description = "활성 여부", example = "true")
        Boolean active
) {
}
