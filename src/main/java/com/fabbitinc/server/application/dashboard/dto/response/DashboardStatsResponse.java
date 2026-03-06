package com.fabbitinc.server.application.dashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "대시보드 통계 응답")
public record DashboardStatsResponse(
        @Schema(description = "Part 통계")
        PartStatsResponse parts,
        @Schema(description = "BOM 통계")
        BomStatsResponse bomLinks,
        @Schema(description = "최근 합성 작업 정보")
        LastSynthesisResponse lastSynthesis
) {
}
