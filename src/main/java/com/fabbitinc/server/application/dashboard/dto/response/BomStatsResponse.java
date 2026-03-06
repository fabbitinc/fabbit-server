package com.fabbitinc.server.application.dashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "BOM 링크 통계")
public record BomStatsResponse(
        @Schema(description = "전체 BOM 링크 수", example = "512")
        int total
) {
}
