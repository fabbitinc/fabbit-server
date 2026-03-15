package com.fabbitinc.server.presentation.mapping.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "매핑 영향 요약")
public record MappingImpactSummaryResponse(
        @Schema(description = "비활성(미사용) 컬럼 수")
        int disabledColumnCount
) {
}
