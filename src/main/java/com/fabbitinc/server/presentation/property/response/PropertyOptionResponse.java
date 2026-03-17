package com.fabbitinc.server.presentation.property.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "속성 옵션 응답")
public record PropertyOptionResponse(
        @Schema(description = "옵션 값", example = "AL6061")
        String value,

        @Schema(description = "옵션 표시명", example = "AL6061")
        String label,

        @Schema(description = "표시 순서", example = "10")
        Integer displayOrder,

        @Schema(description = "활성 여부", example = "true")
        Boolean active
) {
}
