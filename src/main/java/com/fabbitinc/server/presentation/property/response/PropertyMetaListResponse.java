package com.fabbitinc.server.presentation.property.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "속성 메타 목록 응답")
public record PropertyMetaListResponse(
        @Schema(description = "총 건수", example = "12")
        int total,

        @Schema(description = "속성 메타 목록")
        List<PropertyMetaResponse> items
) {
}
